// Clase que implementa la interfaz remota Seed.
// Actúa como un servidor que ofrece un método remoto para leer los bloques
// del fichero publicado.
// LA FUNCIONALIDAD DE LA CLASE SE COMPLETA EN FASE 1 (TODO 1) Y LA 2 (TODO 2)

package peers;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import interfaces.Seed;
import interfaces.Tracker;

// Se comporta como un objeto remoto: UnicastRemoteObject
public class Publisher extends UnicastRemoteObject implements Seed {
    public static final long serialVersionUID=1234567890L;
    String name; // nombre del nodo (solo para depurar)
    String file;
    String path; // convenio: path = name + "/" + file
    int blockSize;
    int numBlocks;
    transient RandomAccessFile raf;

    public Publisher(String n, String f, int bSize) throws RemoteException, IOException {
        name = n; // nombre del nodo (solo para depurar)
        file = f; // nombre del fichero especificado
        path = name + "/" + file; // convenio: directorio = nombre del nodo
        blockSize = bSize; // tamaño de bloque especificado
	// Cálculo del nº bloques redondeado por exceso:
	//     truco: ⌈x/y⌉ -> (x+y-1)/y
        numBlocks = (int) (new File(path).length() + blockSize - 1)/blockSize;

        // TODO 2 v: abrir el fichero para leer (RandomAccessFile)
        raf = new RandomAccessFile(path, "r");

    }
    public String getName() throws RemoteException {
        return name;
    }
    public byte [] read(int numBl) throws RemoteException {
        try { //???
            if (numBl < numBlocks) {
                System.out.println("publisher read " + numBl);

                // TODO 2 v: realiza lectura solicitada devolviendo lo leído en buf 
                // Cuidado con último bloque que probablemente no estará completo
                long fileSizeBytes = new File(path).length();
                int bufSize = blockSize;
                if (numBl + 1 == numBlocks) { // último bloque
                    int fragmentSize = (int) (fileSizeBytes % blockSize);
                    if (fragmentSize > 0) bufSize = fragmentSize;
                }
                byte [] buf = new byte[bufSize];
                raf.seek(numBl * blockSize);
                raf.read(buf);

                return buf;
            } else
                return null;
        }
        catch (Exception e) {
            return null; //???
        }
    }
    public int getNumBlocks() { // no es método remoto
        return numBlocks;
    }

    // Obtiene del registry una referencia al tracker y publica mediante
    // announceFile el fichero especificado creando una instancia de esta clase
    static public void main(String args[]) throws RemoteException {
        if (args.length!=5) {
            System.err.println("Usage: Publisher registryHost registryPort name file blockSize");
            return;
        }
        //if (System.getSecurityManager() == null)
        //    System.setSecurityManager(new SecurityManager());

        try {
            // TODO 1 v: localiza el registry en el host y puerto indicado
            Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            // y obtiene la referencia remota al tracker asignándola
            // a esta variable:
            Tracker trck = (Tracker) registry.lookup("BitCascade");

            // comprobamos si ha obtenido bien la referencia:
            System.out.println("el nombre del nodo del tracker es: " + trck.getName());
            // TODO 1 v: crea un objeto de la clase Publisher y usa el método
            // remoto announceFile del Tracker para publicar el fichero
            // (nº bloques disponible en getNumBlocks de esa clase)
            //
            Publisher pub = new Publisher(args[2], args[3], Integer.parseInt(args[4]));
            boolean res = trck.announceFile(pub, args[3], Integer.parseInt(args[4]), pub.getNumBlocks());
            
            if (!res) { // comprueba resultado
                // si false: ya existe fichero publicado con ese nombre
                System.err.println("Fichero ya publicado");
                System.exit(1);
            }
            System.err.println("Dando servicio...");
            // no termina nunca (modo de operación de UnicastRemoteObject)
        }
        catch (Exception e) {
            System.err.println("Publisher exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
