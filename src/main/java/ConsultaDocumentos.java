import java.io.BufferedReader;
import java.io.FileReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConsultaDocumentos {

    static class DocumentoTermino {
        int frecuencia;
        double tf;
        String docId;
        String titulo;

        DocumentoTermino(int frecuencia, double tf, String docId, String titulo) {
            this.frecuencia = frecuencia;
            this.tf = tf;
            this.docId = docId;
            this.titulo = titulo;
        }
    }

    static class DocumentoSimilar {
        double similitud;
        String docId;
        String titulo;

        DocumentoSimilar(double similitud, String docId, String titulo) {
            this.similitud = similitud;
            this.docId = docId;
            this.titulo = titulo;
        }
    }

    public static String normalizar(String texto) {
        texto = texto.toLowerCase();
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return texto;
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Uso:");
            System.out.println("java ConsultaDocumentos termino");
            return;
        }

        String termino = normalizar(args[0]);

        String archivoTFIDF = "salidas_locales/tf_idf.txt";
        String archivoSimilitud = "salidas_locales/similitud.txt";

        List<DocumentoTermino> documentos = new ArrayList<>();

        try {
            BufferedReader lector = new BufferedReader(new FileReader(archivoTFIDF));
            String linea;

            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split("\t");

                if (partes.length < 4) {
                    continue;
                }

                String palabra = partes[0];

                if (palabra.equals(termino)) {
                    String[] registros = partes[3].split(";");

                    for (String registro : registros) {
                        String[] campos = registro.split("\\|", 7);

                        if (campos.length == 7) {
                            String docId = campos[0];
                            int frecuencia = Integer.parseInt(campos[1]);
                            double tf = Double.parseDouble(campos[3]);
                            String titulo = campos[6];

                            documentos.add(new DocumentoTermino(frecuencia, tf, docId, titulo));
                        }
                    }
                }
            }

            lector.close();

        } catch (Exception e) {
            System.out.println("Error al leer el archivo TF-IDF:");
            System.out.println(e.getMessage());
            return;
        }

        if (documentos.size() == 0) {
            System.out.println("No se encontró el término: " + termino);
            return;
        }

        Collections.sort(documentos, (a, b) -> Integer.compare(b.frecuencia, a.frecuencia));

        DocumentoTermino documentoBase = documentos.get(0);

        System.out.println("DOCUMENTO CON MAYOR OCURRENCIA");
        System.out.println("--------------------------------");
        System.out.println("Término consultado: " + termino);
        System.out.println("Documento: " + documentoBase.docId);
        System.out.println("Frecuencia: " + documentoBase.frecuencia);
        System.out.println("TF: " + documentoBase.tf);
        System.out.println("Título: " + documentoBase.titulo);
        System.out.println();

        List<DocumentoSimilar> similares = new ArrayList<>();

        try {
            BufferedReader lector = new BufferedReader(new FileReader(archivoSimilitud));
            String linea;

            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split("\t");

                if (partes.length < 5) {
                    continue;
                }

                String docA = partes[0];
                String docB = partes[1];
                double coseno = Double.parseDouble(partes[2]);
                String tituloA = partes[3];
                String tituloB = partes[4];

                if (docA.equals(documentoBase.docId)) {
                    similares.add(new DocumentoSimilar(coseno, docB, tituloB));
                }

                if (docB.equals(documentoBase.docId)) {
                    similares.add(new DocumentoSimilar(coseno, docA, tituloA));
                }
            }

            lector.close();

        } catch (Exception e) {
            System.out.println("Error al leer el archivo de similitud:");
            System.out.println(e.getMessage());
            return;
        }

        Collections.sort(similares, (a, b) -> Double.compare(b.similitud, a.similitud));

        System.out.println("DOCUMENTOS MÁS SIMILARES");
        System.out.println("-------------------------");

        if (similares.size() == 0) {
            System.out.println("No se encontraron documentos similares.");
        } else {
            int limite = Math.min(10, similares.size());

            for (int i = 0; i < limite; i++) {
                DocumentoSimilar doc = similares.get(i);

                System.out.println((i + 1) + ". " + doc.docId);
                System.out.println("   Similitud: " + doc.similitud);
                System.out.println("   Título: " + doc.titulo);
                System.out.println();
            }
        }
    }
}