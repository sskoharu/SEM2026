"""Verifica cada fragmento LODC contra el PDF fuente (F01-lodc.pdf).

Uso:
  python tools/verify_corpus_pdf.py            # escribe eval/verificacion_corpus_vs_pdf.md y .json
Un fragmento es VERIFICADO si su texto (sin tildes, espacios ni signos) aparece literalmente en el PDF.
Los demas quedan como DIFIERE o NO_ENCONTRADO con su cobertura, para revision humana.
"""
import difflib, json, os, re, sys, unicodedata
from pypdf import PdfReader

BASE = os.path.join(os.path.dirname(__file__), "..")
DATA = os.path.join(BASE, "src", "main", "resources", "data", "lodc")
PDF = os.path.join(DATA, "original_source", "F01-lodc.pdf")
OUT_MD = os.path.join(BASE, "eval", "verificacion_corpus_vs_pdf.md")
OUT_JSON = os.path.join(BASE, "eval", "verificacion_corpus_vs_pdf.json")


def squash(text):
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]", "", text.lower())


def main():
    pdf = "\n".join((p.extract_text() or "") for p in PdfReader(PDF).pages)
    pdf_sq = squash(pdf)
    rows = []
    for name in sorted(os.listdir(DATA)):
        if not name.endswith(".json"):
            continue
        with open(os.path.join(DATA, name), encoding="utf-8") as f:
            for item in json.load(f):
                frag = squash(item["document"])
                if frag in pdf_sq:
                    status, cov = "VERIFICADO", 1.0
                elif all(squash(p) in pdf_sq for p in re.split(r":\s*(?=\d+\.\s)", item["document"], maxsplit=1)) \
                        and re.search(r":\s*\d+\.\s", item["document"]):
                    # Fragmento de un numeral: encabezado del articulo + numeral, ambos literales en el PDF
                    status, cov = "VERIFICADO", 1.0
                else:
                    head = frag[:40]
                    pos = pdf_sq.find(head)
                    if pos < 0:
                        status, cov = "NO_ENCONTRADO", 0.0
                    else:
                        window = pdf_sq[pos:pos + int(len(frag) * 1.3)]
                        m = difflib.SequenceMatcher(None, frag, window, autojunk=False)
                        cov = sum(b.size for b in m.get_matching_blocks()) / len(frag)
                        status = "DIFIERE"
                rows.append({"id": item["id"], "archivo": name,
                             "articulo": str(item["metadata"].get("numero_articulo", "")),
                             "estado": status, "cobertura": round(cov, 4)})

    ok = [r for r in rows if r["estado"] == "VERIFICADO"]
    with open(OUT_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(rows, f, ensure_ascii=False, indent=1)
    lines = ["# Verificación del corpus LODC contra F01-lodc.pdf", "",
             f"Generado por `tools/verify_corpus_pdf.py`. Fragmentos: {len(rows)}; verificados literalmente: {len(ok)}; "
             f"con diferencias o no encontrados: {len(rows) - len(ok)}.", "",
             "Comparación sin tildes, espacios ni signos (el PDF inserta espacios espurios al extraer texto).", "",
             "| Fragmento | Artículo | Estado | Cobertura |", "|---|---|---|---|"]
    lines += [f"| {r['id']} | {r['articulo']} | {r['estado']} | {r['cobertura']:.0%} |" for r in rows]
    with open(OUT_MD, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print(f"{len(rows)} fragmentos: {len(ok)} verificados, {len(rows) - len(ok)} por revisar")


if __name__ == "__main__":
    sys.exit(main())
