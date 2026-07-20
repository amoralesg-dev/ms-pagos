
#20/07/2026 se agrega campo para referencia manual
###aplicado en dev y prod
ALTER TABLE pagos_archivo
ADD COLUMN referencia_manual VARCHAR(255) NULL;

ALTER TABLE pagos_db.pagos_archivo MODIFY COLUMN informacion_adicional varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;


ALTER TABLE pagos_db.pagos_archivo MODIFY COLUMN referencia_manual varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;
