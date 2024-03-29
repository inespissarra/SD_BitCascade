// Clase que implementa la interfaz remota Seed.
// Actúa como un servidor que ofrece un método remoto para leer los bloques
// del fichero que se está descargando.
// Proporciona un método estático (init) para instanciarla.
// LA FUNCIONALIDAD SE COMPLETA EN LAS 4 FASES TODO 1, TODO 2, TODO 3 y TODO 4
// En las fases 3 y 4 se convertirá en un objeto remoto

package peers;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.rmi.RemoteException;

import interfaces.Seed;
import interfaces.Leech;
import interfaces.Tracker;
import interfaces.FileInfo;
import java.io.RandomAccessFile;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.concurrent.ConcurrentLinkedQueue;


// Un Leech guarda en esta clase auxiliar su conocimiento sobre cuál es el
// último bloque descargado por otro Leech.
class LeechInfo {
    Leech leech;    // de qué Leech se trata
    int lastBlock;  // último bloque que sabemos que se ha descargado
    public LeechInfo (Leech l, int nBl) {
        leech = l;
        lastBlock = nBl;
    }
    Leech getLeech() {
        return leech;
    }
    int getLastBlock() {
        return lastBlock;
    }
    void setLastBlock(int nBl) {
        lastBlock = nBl;
    }
};
// Esta clase actúa solo de cliente en las dos primeras fases, pero
// en las dos últimas ejerce también como servidor convirtiéndose en
// un objeto remoto.
public class DownloaderImpl extends UnicastRemoteObject implements Leech { //???
    public static final long serialVersionUID=1234567890L;
    String name; // nombre del nodo (solo para depurar)
    String file;
    String path;
    int blockSize;
    int numBlocks;
    int lastBlock = -1; // último bloque descargado por este Leech
    Seed seed;
    FileInfo fInfo;
    // TODO 3: lista de leeches posteriores que deben ser notificados
    transient RandomAccessFile raf;
    ArrayList<LeechInfo> prevLeechs;
    Map<Leech, LeechInfo> prevLeechsMap;
    int it = 0;
    ConcurrentLinkedQueue<Leech> postLeechs = new ConcurrentLinkedQueue<Leech>();

    public DownloaderImpl(String n, String f, FileInfo finf) throws RemoteException, IOException {
        name = n;
        file = f;
        path = name + "/" + file;
        blockSize = finf.getBlockSize();
        numBlocks = finf.getNumBlocks();
        seed = finf.getSeed();
        fInfo = finf;
        // TODO 2: abre el fichero para escritura
        raf = new RandomAccessFile(path, "rw");
        raf.setLength(0);

        // TODO 3: obtiene el número del último bloque descargado por leeches
	    // anteriores (contenidos en FileInfo) usando getLastBlockNumber
        prevLeechs = new ArrayList<LeechInfo>();
        for (Leech leech : finf.getLeechList()) {
            prevLeechs.add(new LeechInfo(leech, leech.getLastBlockNumber()));
            // TODO 4: solicita a esos leeches anteriores usando newLeech
            // que le notifiquen cuando progrese su descarga
            leech.newLeech(this);
        }
        prevLeechsMap = prevLeechs.stream().collect(Collectors.toMap(LeechInfo::getLeech, Function.identity()));
    }
    /* métodos locales */
    public int getNumBlocks() {
        return numBlocks;
    }
    public FileInfo getFileInfo() {
        return fInfo;
    }
    // realiza la descarga de un bloque y lo almacena en un fichero local
    public boolean downloadBlock(int numBl) throws RemoteException {
        try{
            byte [] buf = null;
            // TODO 3: Alterna leer bloques del seed y de otros leeches
            while(it < prevLeechs.size()) {
                LeechInfo leechInfo = prevLeechs.get(it);
                it++;
                int lastBlockLeech = leechInfo.getLastBlock();
                if (numBl <= lastBlockLeech) {
                    Leech leech = leechInfo.getLeech();
                    buf = leech.read(numBl);
                    break;
                }
            }
            if (it == prevLeechs.size() && buf == null){
                it = 0;
                // TODO 2: Lee bloque del seed y lo escribe en el fichero
                buf = seed.read(numBl);
            }
            if (buf == null) return false;
            raf.seek(numBl * blockSize);
            raf.write(buf);
            lastBlock = numBl;
	        // TODO 4: Notifica a los leeches posteriores (notifyBlock)
            for (Leech leech : postLeechs) 
                leech.notifyBlock(this, numBl);
                
            return true;
        } catch  (Exception e) {
            System.out.println("algo correu mal :(");
            return false;
        }
    }

    /* métodos remotos que solo se usarán cuando se convierta en
       un objeto remoto en la fase 3 */
 
    // solo para depurar
    public String getName() throws RemoteException {
        return name;
    }
    // prácticamente igual que read del Seed
    public byte [] read(int numBl) throws RemoteException { 
        try { //???
            if (numBl <= lastBlock) {
                // TODO 3: realiza lectura solicitada devolviendo lo leído en buf 
                // Cuidado con último bloque que probablemente no estará completo
                long fileSizeBytes = new File(path).length();
                System.out.println("downloader fileSizeBytes " + path + " " + fileSizeBytes);
                int bufSize = blockSize;
                if (numBl == lastBlock) { // último bloque numBlocks
                    int fragmentSize = (int) (fileSizeBytes % blockSize);
                    if (fragmentSize > 0) bufSize = fragmentSize;
                }
                byte [] buf = new byte[bufSize];
                raf.seek(numBl * blockSize);
                raf.read(buf);

                return buf;
            } else
                return null;
        } catch  (Exception e) { //???
            return null;
        }
    } 
    // obtiene cuál es el último bloque descargado por este Leech
    public int getLastBlockNumber() throws RemoteException{
        return lastBlock;
    }
    /* métodos remotos solo para la última fase */
    // leech solicitante será notificado del progreso de la descarga
    public void newLeech(Leech requester) throws RemoteException {
        // TODO 4: añade ese leech a la lista de leeches posteriores
	    // que deben ser notificados
        postLeechs.add(requester);
    }
    // Informa del progreso de la descarga
    public void notifyBlock(Leech l, int nBl) throws RemoteException {
        // TODO 4: actualizamos la información sobre el último bloque
	    // descargado por ese leech
        LeechInfo leechInfo = prevLeechsMap.get(l);
        leechInfo.setLastBlock(nBl);
    }

    // método estático que obtiene del registry una referencia al tracker y
    // obtiene mediante lookupFile la información del fichero especificado
    // creando una instancia de esta clase
    static public DownloaderImpl init(String host, int port, String name, String file) throws RemoteException {
        //if (System.getSecurityManager() == null)
        //    System.setSecurityManager(new SecurityManager());

        DownloaderImpl down = null;
        try {
            // TODO 1: localiza el registry en el host y puerto indicado
            Registry registry = LocateRegistry.getRegistry(host, port);
            // y obtiene la referencia remota al tracker asignándola
            // a esta variable:
            Tracker trck = (Tracker) registry.lookup("BitCascade");

            // comprobamos si ha obtenido bien la referencia:
            System.out.println("el nombre del nodo del tracker es: " + trck.getName());

            // TODO 1: obtiene la información del fichero mediante el
	        // método lookupFile del Tracker.
            FileInfo finf = trck.lookupFile(file); // asigna resultado de lookupFile

            if (finf==null) { // comprueba resultado
                // si null: no se ha publicado ese fichero
                System.err.println("Fichero no publicado");
                System.exit(1);
            }
            // TODO 1: crea un objeto de la clase DownloaderImpl
            down = new DownloaderImpl(name, file, finf);

            // TODO 3: usa el método addLeech del tracker para añadirse
            trck.addLeech(down, file);
        }
        catch (Exception e) {
            System.err.println("Downloader exception:");
            e.printStackTrace();
            System.exit(1);
        }
        return down;
    }
}
