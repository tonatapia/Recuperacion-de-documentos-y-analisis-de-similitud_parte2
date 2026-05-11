import java.io.BufferedReader;
import java.io.FileReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sistema de Consulta de Documentos Similares
 *
 * Uso:
 *   java ConsultaDocumentos <termino>
 *
 * Archivos esperados en salidas_locales/:
 *   tf_idf.txt   — salida del job TF-IDF
 *   similitud.txt — salida del job Cosine Similarity
 *
 * Formato tf_idf.txt (una línea por palabra):
 *   palabra \t idf \t numDocs \t docId|freq|total|tf|idf|tfidf|titulo;...
 *
 * Formato similitud.txt (una línea por par):
 *   docA \t docB \t coseno \t tituloA \t tituloB
 */
public class ConsultaDocumentos {

    // ------------------------------------------------------------------ modelos

    static class DocumentoTermino {
        String docId;
        String titulo;
        int    frecuencia;
        int    totalPalabras;
        double tf;
        double idf;
        double tfidf;

        DocumentoTermino(String docId, String titulo,
                         int frecuencia, int totalPalabras,
                         double tf, double idf, double tfidf) {
            this.docId        = docId;
            this.titulo       = titulo;
            this.frecuencia   = frecuencia;
            this.totalPalabras= totalPalabras;
            this.tf           = tf;
            this.idf          = idf;
            this.tfidf        = tfidf;
        }
    }

    static class DocumentoSimilar {
        String docId;
        String titulo;
        double similitud;

        DocumentoSimilar(String docId, String titulo, double similitud) {
            this.docId    = docId;
            this.titulo   = titulo;
            this.similitud= similitud;
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Convierte a minúsculas y elimina acentos */
    static String normalizar(String texto) {
        texto = texto.toLowerCase();
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return texto;
    }

    /** Imprime una línea separadora */
    static void linea(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        System.out.println(sb.toString());
    }

    // ------------------------------------------------------------------ lectura TF-IDF

    /**
     * Lee tf_idf.txt y devuelve todos los documentos que contienen el término.
     *
     * Formato de cada línea:
     *   palabra \t idf \t numDocs \t posting
     *
     * Formato de cada entrada en el posting (separadas por ';'):
     *   docId|freq|total|tf|idf|tfidf|titulo
     */
    static List<DocumentoTermino> buscarTermino(String termino, String archivo) {

        List<DocumentoTermino> resultado = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String linea;
            while ((linea = br.readLine()) != null) {

                String[] partes = linea.split("\t", 4);
                if (partes.length < 4) continue;

                String palabra = normalizar(partes[0].trim());
                if (!palabra.equals(termino)) continue;

                // Línea encontrada — procesar posting
                String posting = partes[3];

                for (String entrada : posting.split(";")) {

                    String[] campos = entrada.split("\\|", 7);
                    if (campos.length < 6) continue;

                    String docId        = campos[0];
                    int    frecuencia   = Integer.parseInt(campos[1]);
                    int    totalPalabras= Integer.parseInt(campos[2]);
                    double tf           = Double.parseDouble(campos[3]);
                    double idf          = Double.parseDouble(campos[4]);
                    double tfidf        = Double.parseDouble(campos[5]);
                    String titulo       = campos.length == 7 ? campos[6] : "(sin título)";

                    resultado.add(new DocumentoTermino(
                        docId, titulo, frecuencia, totalPalabras, tf, idf, tfidf));
                }

                break; // cada término aparece una sola vez en el índice
            }

        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo leer " + archivo + ": " + e.getMessage());
        }

        return resultado;
    }

    // ------------------------------------------------------------------ lectura similitud

    /**
     * Lee similitud.txt y devuelve los documentos similares al docId dado.
     *
     * Formato de cada línea:
     *   docA \t docB \t coseno \t tituloA \t tituloB
     */
    static List<DocumentoSimilar> buscarSimilares(String docIdBase, String archivo) {

        List<DocumentoSimilar> similares = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String linea;
            while ((linea = br.readLine()) != null) {

                String[] p = linea.split("\t", 5);
                if (p.length < 5) continue;

                String docA   = p[0].trim();
                String docB   = p[1].trim();
                double coseno = Double.parseDouble(p[2].trim());
                String tituloA = p[3];
                String tituloB = p[4];

                if (docA.equals(docIdBase)) {
                    similares.add(new DocumentoSimilar(docB, tituloB, coseno));
                } else if (docB.equals(docIdBase)) {
                    similares.add(new DocumentoSimilar(docA, tituloA, coseno));
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo leer " + archivo + ": " + e.getMessage());
        }

        return similares;
    }

    // ------------------------------------------------------------------ verificación

    /**
     * Verifica que cada documento similar comparta al menos una palabra
     * significativa con el documento base buscando en tf_idf.txt.
     * Devuelve el número de palabras compartidas (palabras con tfidf > 0).
     */
    static int contarPalabrasCompartidas(String docBase, String docSimilar,
                                          String archivoTFIDF) {
        int compartidas = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivoTFIDF))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split("\t", 4);
                if (partes.length < 4) continue;
                String posting = partes[3];

                boolean tieneBase    = false;
                boolean tieneSimilar = false;

                for (String entrada : posting.split(";")) {
                    String[] campos = entrada.split("\\|", 2);
                    if (campos.length < 1) continue;
                    String docId = campos[0];
                    if (docId.equals(docBase))    tieneBase    = true;
                    if (docId.equals(docSimilar)) tieneSimilar = true;
                    if (tieneBase && tieneSimilar) break;
                }

                if (tieneBase && tieneSimilar) compartidas++;
            }
        } catch (Exception e) {
            // silencioso — la verificación es opcional
        }
        return compartidas;
    }

    // ------------------------------------------------------------------ etiqueta similitud

    /** Devuelve una etiqueta descriptiva del nivel de similitud */
    static String etiquetaSimilitud(double coseno) {
        if (coseno >= 0.80) return "MUY ALTA  ████████";
        if (coseno >= 0.60) return "ALTA      ██████░░";
        if (coseno >= 0.40) return "MEDIA     ████░░░░";
        if (coseno >= 0.20) return "BAJA      ██░░░░░░";
        return                     "MUY BAJA  █░░░░░░░";
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) {

        // ── validar argumentos ──────────────────────────────────────────────
        if (args.length < 1) {
            System.out.println("Uso: java ConsultaDocumentos <termino>");
            System.out.println("Ejemplo: java ConsultaDocumentos mexico");
            return;
        }

        String termino        = normalizar(args[0].trim());
        String archivoTFIDF   = "salidas_locales/tf_idf.txt";
        String archivoSimilitud = "salidas_locales/similitud.txt";
        int    topSimilares   = 10; // cuántos documentos similares mostrar

        // ── encabezado ──────────────────────────────────────────────────────
        linea('=', 65);
        System.out.println("  SISTEMA DE CONSULTA DE DOCUMENTOS SIMILARES");
        System.out.println("  IPN UPIIT — Big Data");
        linea('=', 65);
        System.out.println("  Término consultado : " + termino);
        System.out.println("  Archivo TF-IDF     : " + archivoTFIDF);
        System.out.println("  Archivo similitud  : " + archivoSimilitud);
        linea('-', 65);

        // ── paso 1: buscar documentos que contienen el término ──────────────
        System.out.println("\n[1/3] Buscando documentos con el término \"" + termino + "\"...");
        List<DocumentoTermino> documentos = buscarTermino(termino, archivoTFIDF);

        if (documentos.isEmpty()) {
            System.out.println("\n  No se encontró el término \"" + termino
                + "\" en el índice TF-IDF.");
            System.out.println("  Sugerencia: verifica ortografía o prueba con otra palabra.");
            return;
        }

        System.out.println("  Documentos encontrados: " + documentos.size());

        // ── paso 2: ordenar por frecuencia y mostrar top 5 ──────────────────
        Collections.sort(documentos,
            (a, b) -> Integer.compare(b.frecuencia, a.frecuencia));

        System.out.println("\n[2/3] Ranking de documentos por frecuencia del término:");
        linea('-', 65);
        System.out.printf("  %-4s %-12s %-6s %-10s %-10s%n",
            "#", "DOC-ID", "FREQ", "TF", "TF-IDF");
        linea('-', 65);

        int mostrar = Math.min(5, documentos.size());
        for (int i = 0; i < mostrar; i++) {
            DocumentoTermino d = documentos.get(i);
            System.out.printf("  %-4d %-12s %-6d %-10.4f %-10.4f%n",
                i + 1, d.docId, d.frecuencia, d.tf, d.tfidf);
            System.out.printf("       Título: %s%n",
                d.titulo.isEmpty() ? "(sin título)" : d.titulo);
        }

        // ── documento base = mayor frecuencia ───────────────────────────────
        DocumentoTermino base = documentos.get(0);

        linea('-', 65);
        System.out.println("\n  ★ DOCUMENTO BASE (mayor ocurrencia)");
        System.out.println("    ID       : " + base.docId);
        System.out.println("    Título   : " + (base.titulo.isEmpty()
                            ? "(sin título)" : base.titulo));
        System.out.println("    Frecuencia: " + base.frecuencia
                            + " / " + base.totalPalabras + " palabras");
        System.out.printf ("    TF       : %.6f%n", base.tf);
        System.out.printf ("    IDF      : %.6f%n", base.idf);
        System.out.printf ("    TF-IDF   : %.6f%n", base.tfidf);

        // ── paso 3: buscar y mostrar documentos similares ───────────────────
        System.out.println("\n[3/3] Buscando documentos similares al documento base...");
        List<DocumentoSimilar> similares = buscarSimilares(base.docId, archivoSimilitud);

        if (similares.isEmpty()) {
            System.out.println("\n  No se encontraron documentos similares para: "
                + base.docId);
            System.out.println("  Verifica que el job de Similitud Coseno se ejecutó.");
            return;
        }

        Collections.sort(similares,
            (a, b) -> Double.compare(b.similitud, a.similitud));

        linea('=', 65);
        System.out.println("  DOCUMENTOS MÁS SIMILARES — Top " + topSimilares);
        System.out.println("  Base: " + base.docId
            + " | " + (base.titulo.isEmpty() ? "(sin título)" : base.titulo));
        linea('=', 65);

        int limite = Math.min(topSimilares, similares.size());
        for (int i = 0; i < limite; i++) {
            DocumentoSimilar doc = similares.get(i);

            // verificación: cuántas palabras comparten
            int compartidas = contarPalabrasCompartidas(
                base.docId, doc.docId, archivoTFIDF);

            System.out.printf("%n  %d. %s%n", i + 1, doc.docId);
            System.out.println("     Título     : "
                + (doc.titulo.isEmpty() ? "(sin título)" : doc.titulo));
            System.out.printf("     Similitud  : %.4f  [%s]%n",
                doc.similitud, etiquetaSimilitud(doc.similitud));
            System.out.println("     Palabras en común (verificación): "
                + compartidas);

            // advertencia si similitud es muy baja con pocas palabras en común
            if (doc.similitud < 0.10 && compartidas < 3) {
                System.out.println("     ⚠ Similitud marginal — pocas palabras compartidas");
            }
        }

        // ── resumen final ────────────────────────────────────────────────────
        linea('=', 65);
        System.out.printf("%nResumen:%n");
        System.out.println("  Término buscado       : " + termino);
        System.out.println("  Documento base        : " + base.docId);
        System.out.println("  Docs con el término   : " + documentos.size());
        System.out.println("  Docs similares totales: " + similares.size());
        System.out.println("  Mostrando top         : " + limite);

        double promedioSim = 0;
        for (int i = 0; i < limite; i++) promedioSim += similares.get(i).similitud;
        promedioSim /= limite;
        System.out.printf ("  Similitud promedio    : %.4f%n", promedioSim);
        linea('=', 65);
    }
}