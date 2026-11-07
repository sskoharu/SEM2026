"""Genera/actualiza src/main/resources/data/corpus_manifest.json.

Es la unica via para registrar hashes y estados de revision del corpus.
Uso:
  python tools/corpus_manifest.py                      # recalcula hashes; conserva estados vigentes
  python tools/corpus_manifest.py --approve 20,26 --reviewer "Nombre"   # aprueba articulos LODC
  python tools/corpus_manifest.py --approve-all --reviewer "Nombre"     # solo tras revisar todo contra la fuente
  python tools/corpus_manifest.py --check              # falla si JSON y manifiesto divergen
Un cambio de texto invalida el hash y devuelve el fragmento a PENDIENTE_REVISION.
"""
import argparse, datetime, glob, hashlib, json, os, sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data")
MANIFEST = os.path.join(ROOT, "corpus_manifest.json")
VERIFICATION = os.path.join(os.path.dirname(__file__), "..", "eval", "verificacion_corpus_vs_pdf.json")
PENDING, APPROVED = "PENDIENTE_REVISION", "APROBADO"


def sha(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def load_fragments():
    out = []
    for pattern in ("lodc/*.json", "dpe/*.json"):
        for path in sorted(glob.glob(os.path.join(ROOT, pattern))):
            rel = os.path.relpath(path, ROOT).replace("\\", "/")
            with open(path, encoding="utf-8") as f:
                for item in json.load(f):
                    out.append((rel, item))
    return out


def corpus_version(fragments):
    joined = "\n".join(f"{k}:{fragments[k]['sha256']}" for k in sorted(fragments))
    return sha(joined)[:16]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--approve", help="numeros de articulo LODC separados por coma")
    ap.add_argument("--approve-all", action="store_true")
    ap.add_argument("--approve-verified", action="store_true",
                    help="aprueba los fragmentos VERIFICADOS en eval/verificacion_corpus_vs_pdf.json")
    ap.add_argument("--reviewer")
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()

    old = {}
    if os.path.exists(MANIFEST):
        with open(MANIFEST, encoding="utf-8") as f:
            old = json.load(f).get("fragments", {})

    verified = set()
    if a.approve_verified:
        with open(VERIFICATION, encoding="utf-8") as f:
            verified = {r["id"] for r in json.load(f) if r["estado"] == "VERIFICADO"}

    approve = set((a.approve or "").split(",")) - {""}
    if (approve or a.approve_all or a.approve_verified) and not a.reviewer:
        sys.exit("--reviewer es obligatorio para aprobar")

    frags = {}
    for rel, item in load_fragments():
        fid, digest = item["id"], sha(item["document"])
        prev = old.get(fid, {})
        entry = {"file": rel, "sha256": digest, "estado_revision": PENDING,
                 "revisado_por": None, "fecha_revision": None}
        if prev.get("sha256") == digest and prev.get("estado_revision") == APPROVED:
            entry.update({k: prev[k] for k in ("estado_revision", "revisado_por", "fecha_revision")})
        art = str(item.get("metadata", {}).get("numero_articulo", ""))
        if a.approve_all or fid in verified or (fid.startswith("lodc_ley_art_") and art in approve):
            entry.update({"estado_revision": APPROVED, "revisado_por": a.reviewer,
                          "fecha_revision": datetime.date.today().isoformat()})
        frags[fid] = entry

    if a.check:
        bad = [k for k in frags if old.get(k, {}).get("sha256") != frags[k]["sha256"]]
        bad += [k for k in old if k not in frags]
        if bad:
            sys.exit(f"Manifiesto desactualizado/divergente: {bad[:10]} ...")
        print("OK", len(frags), "fragmentos")
        return

    approved = sum(1 for v in frags.values() if v["estado_revision"] == APPROVED)
    manifest = {"corpus_version": corpus_version(frags), "total": len(frags),
                "aprobados": approved, "pendientes": len(frags) - approved, "fragments": frags}
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1)
    print(f"{len(frags)} fragmentos, {approved} aprobados, version {manifest['corpus_version']}")


if __name__ == "__main__":
    main()
