
#20/07/2026 se agrega campo para referencia manual
###aplicado en dev y prod
ALTER TABLE pagos_archivo
ADD COLUMN referencia_manual VARCHAR(255) NULL;