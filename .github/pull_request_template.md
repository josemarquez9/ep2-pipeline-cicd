## ¿Qué cambia?

<!-- Describa el cambio en 2-3 líneas y enlace el issue relacionado (Closes #...). -->

## ¿Cómo se probó?

- [ ] `mvn verify` (productos-java)
- [ ] `pytest` (reportes-python)
- [ ] `docker compose up -d --build --wait` + `deploy/scripts/smoke-test.sh`

## Checklist

- [ ] El pipeline está en verde (pruebas, seguridad, imágenes)
- [ ] No se agregan secretos ni archivos `.env`
- [ ] Se actualizó la documentación y el `CHANGELOG.md` si corresponde
- [ ] Impacto en despliegue o seguridad: <!-- ninguno / describir -->
