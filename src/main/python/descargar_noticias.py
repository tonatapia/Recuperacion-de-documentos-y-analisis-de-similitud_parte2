import requests
import json
import time
import re
import os
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse
import nltk
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords

# Descargar recursos de NLTK (solo la primera vez)
nltk.download('punkt', quiet=True)
nltk.download('stopwords', quiet=True)
nltk.download('punkt_tab', quiet=True)

# Configuración
BASE = "https://www.eluniversal.com.mx"
HEADERS = {"User-Agent": "Mozilla/5.0"}
PALABRAS_FILTRO = {'publicidad', 'anuncio', 'suscribete'}  # palabras extra a eliminar
MAX_ARTICULOS = 200
MAX_PAGINAS = 50

# Cargar stopwords en español y agregar las extra
stop_words = set(stopwords.words('spanish'))
stop_words.update(PALABRAS_FILTRO)

def limpiar_basico(texto):
    texto = texto.replace("\n", " ").replace("\t", " ")
    return " ".join(texto.split())

def limpieza_profunda(texto):

    # Convertir a minúsculas
    texto = texto.lower()
    
    # Eliminar todo excepto letras, acentos, ñ y espacios
    texto = re.sub(r'[^a-záéíóúñü\s]', '', texto)
    
    # Tokenizar
    tokens = word_tokenize(texto, language='spanish')
    
    # Filtrar tokens
    tokens_filtrados = [
        token for token in tokens
        if token not in stop_words and len(token) > 2
    ]
    
    # Reconstruir texto
    return " ".join(tokens_filtrados)

def url_lista(pagina):
    if pagina == 1:
        return BASE + "/minuto-x-minuto/"
    else:
        return BASE + f"/minuto-x-minuto/todos/{pagina}/"

def es_articulo(url, titulo):
    datos_url = urlparse(url)
    if "eluniversal.com.mx" not in datos_url.netloc:
        return False
    ruta = datos_url.path.strip("/")
    if ruta == "":
        return False
    if ruta.startswith("minuto-x-minuto"):
        return False
    if len(titulo) < 20:
        return False
    partes = ruta.split("/")
    if len(partes) < 2:
        return False
    return True

def obtener_links():
    links = []
    vistos = set()
    pagina = 1
    
    while len(links) < MAX_ARTICULOS and pagina <= MAX_PAGINAS:
        url = url_lista(pagina)
        print(f"Leyendo página: {url}")
        
        try:
            respuesta = requests.get(url, headers=HEADERS, timeout=15)
            sopa = BeautifulSoup(respuesta.text, "html.parser")
            
            for a in sopa.find_all("a", href=True):
                titulo = limpiar_basico(a.get_text())
                link = urljoin(BASE, a["href"])
                
                if es_articulo(link, titulo) and link not in vistos:
                    vistos.add(link)
                    links.append((link, titulo))
                    print(f"  Encontrado: {len(links)} - {titulo[:60]}")
                    
                if len(links) >= MAX_ARTICULOS:
                    break
                    
        except Exception as error:
            print(f"Error en página {pagina}: {error}")
        
        pagina += 1
        time.sleep(0.5)
    
    return links

def obtener_articulo(url, titulo_lista):
    try:
        respuesta = requests.get(url, headers=HEADERS, timeout=15)
        sopa = BeautifulSoup(respuesta.text, "html.parser")
        
        # Extraer título desde <h1> si existe, si no usar el de la lista
        h1 = sopa.find("h1")
        titulo = limpiar_basico(h1.get_text()) if h1 else titulo_lista
        
        # Extraer párrafos
        parrafos = []
        for p in sopa.find_all("p"):
            texto_parrafo = limpiar_basico(p.get_text())
            # Filtrar párrafos muy cortos, copyright o menciones del diario
            if len(texto_parrafo.split()) >= 6:
                if "Copyright" not in texto_parrafo and "EL UNIVERSAL" not in texto_parrafo:
                    parrafos.append(texto_parrafo)
        
        texto_completo = " ".join(parrafos)
        
        # Si el texto resultante es muy corto, usar el título como respaldo
        if len(texto_completo.split()) < 30:
            texto_completo = titulo
        
        # APLICAR LIMPIEZA PROFUNDA (eliminar stopwords, publicidad, etc.)
        texto_limpio = limpieza_profunda(texto_completo)
        
        return titulo, texto_limpio
    
    except Exception as e:
        print(f"  Error al obtener artículo {url}: {e}")
        return titulo_lista, ""

# Crear directorio de salida si no existe
os.makedirs("datos", exist_ok=True)

# Obtener los enlaces
print("RECOGIENDO ENLACES")
links = obtener_links()
print(f"\nTotal de enlaces encontrados: {len(links)}")
print("DESCARGANDO Y LIMPIANDO ARTÍCULOS\n")

# Procesar cada enlace y guardar en JSONL
with open("datos/documentos.jsonl", "w", encoding="utf-8") as archivo:
    contador = 1
    for url, titulo_lista in links:
        titulo, texto_limpio = obtener_articulo(url, titulo_lista)
        
        # Solo guardar si el texto limpio no está vacío
        if texto_limpio:
            documento = {
                "id": f"doc{contador:03d}",
                "titulo": titulo,
                "url": url,
                "texto": texto_limpio   # Texto ya limpio (minúsculas, sin stopwords, sin publicidad)
            }
            archivo.write(json.dumps(documento, ensure_ascii=False) + "\n")
            print(f"Guardado: {documento['id']} - {titulo[:60]}")
            contador += 1
        else:
            print(f"Saltando (texto vacío): {titulo[:60]}")
        
        time.sleep(0.5)

print(f"Total de noticias descargadas y limpiadas: {contador-1}")
print("Archivo generado: datos/documentos.jsonl")