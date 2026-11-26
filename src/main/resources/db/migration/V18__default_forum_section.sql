INSERT INTO forum_section(name, sort_order, status)
SELECT '综合交流', 0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM forum_section);
