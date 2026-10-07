import hashlib
import sys

from cryptography import x509


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: cert_hash.py <PEM certificate>", file=sys.stderr)
        return 2

    with open(sys.argv[1], "rb") as cert_file:
        certificate = x509.load_pem_x509_certificate(cert_file.read())

    digest = hashlib.md5(certificate.subject.public_bytes()).digest()
    print(digest[:4][::-1].hex())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
