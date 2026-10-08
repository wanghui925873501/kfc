-- 在手工执行 V1 至 V5 后执行一次；服务启动不会自动执行本脚本。
-- 下面的查询必须不返回记录；若存在重复手机号，应先人工核对并合并，不能直接删除 token 数据。
SELECT brand, phone_plain, COUNT(*) AS duplicate_count
FROM kfc_user
WHERE phone_plain IS NOT NULL
GROUP BY brand, phone_plain
HAVING COUNT(*) > 1;

-- 手机号成为内部接口统一通行标识后，用唯一索引保证一次查询最多得到一条 KFC 用户映射。
ALTER TABLE kfc_user
    ADD UNIQUE KEY uk_kfc_user_brand_phone_plain (brand, phone_plain);
