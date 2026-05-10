import requests
import json
import time 

from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse

BASE = "https://www.eluniversal.com.mx"

headers = {
    "User-Agent": "Mozilla/5.0"
}

def limpiar(texto):
    texto = texto.replace("\n", " ")
    texto = texto.replace("\t", " ")
    texto = " ".join(texto.split())
    return texto

def url_lista(pagina):
    if pagina ==1:
        return BASE + "/minuto-x-minuto/"
    else:
        return BASE + f"/minuto-x-minuto/todos/{pagina}/"


def es_articulo(url, titulo):
    datos_url=urlparse(url)

    if "eluniversal.com.mx" not in datos_url.netloc:
        return False
    
    ruta = datos_url.path.strip("/")

    if ruta == "":
        return False
    
    if ruta.startswith("minuto-x-minuto"):
        return False
    
    if len(titulo) < 20:
        return False
    
    partes=ruta.split("/")

    if len(partes) < 2:
        return False
    
    return True

def obtener_links():
    links=[]
    vistos=set()
    pagina=1

    while len(links) < 200 and pagina <= 50:
        url=url_lista(pagina)
        print("leyendo pagina: ", url)

        try:
            respuesta=requests.get(url, headers=headers, timeout=15)
            sopa = BeautifulSoup(respuesta.text, "html.parser")

            for a in sopa.find_all("a", href=True):
                titulo = limpiar(a.get_text())
                link = urljoin(BASE, a["href"])

                if es_articulo(link, titulo) and link not in vistos:
                    vistos.add(link)
                    links.append((link, titulo))
                    print("encontrada:", len(links), titulo)

                if len(links) >= 200:
                    break
        except Exception as error:
            print("error en página:", pagina, error)

        pagina = pagina + 1
        time.sleep(0.5)

    return links

def obtener_articulo(url, titulo_lista):
    try:
        respuesta = requests.get(url, headers=headers, timeout=15)
        sopa = BeautifulSoup(respuesta.text, "html.parser")

        titulo = titulo_lista

        h1 = sopa.find("h1")
        if h1:
            titulo = limpiar(h1.get_text())

        parrafos = []

        for p in sopa.find_all("p"):
            texto = limpiar(p.get_text())

            if len(texto.split()) >= 6:
                if "Copyright" not in texto:
                    if "EL UNIVERSAL" not in texto:
                        parrafos.append(texto)

        texto_final = " ".join(parrafos)
        texto_final = limpiar(texto_final)

        if len(texto_final.split()) < 30:
            texto_final = titulo

        return titulo, texto_final

    except Exception:
        return titulo_lista, titulo_lista

links = obtener_links()

with open("datos/documentos.jsonl", "w", encoding="utf-8") as archivo:
    contador = 1

    for url, titulo_lista in links:
        titulo, texto = obtener_articulo(url, titulo_lista)

        documento = {
            "id": f"doc{contador:03d}",
            "titulo": titulo,
            "url": url,
            "texto": texto
        }

        archivo.write(json.dumps(documento, ensure_ascii=False) + "\n")
        print("Guardado:", documento["id"], titulo)

        contador = contador +1 
        time.sleep(0.5)


print("Total de noticias descargadas:", len(links))
print("Archivo generado: datos/documentos.jsonl")

