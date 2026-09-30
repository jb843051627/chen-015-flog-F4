-- flog 伐区申报清册逐行核收 —— 旧库升级脚本（在已建过初版 t_flog_declare_row 的库上执行一遍）。
-- 新库直接执行 flog.sql 即可，勿重复执行本件。
-- 库：chen_015
-- 2026-09 追加（伐区调查设计·强度界并入系统）：
--   1) 界册 t_flog_yield_rule 加 scope_kind（旧库旧线此格先空，归类补录前不参与任何取界——
--      空类别的界一条都不许被请出来，宁可“无线可依”也不拿错类别的线凑）；
--   2) 新建材积表、调查设计单、检尺株账三表；
--   3) 追加界册/材积表/调查设计三段菜单与按钮权限点。

ALTER TABLE t_flog_declare_row
  ADD COLUMN site_no varchar(64) DEFAULT NULL COMMENT '申报编制单位底档字号' AFTER row_no,
  ADD COLUMN subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号' AFTER site_no,
  ADD COLUMN cut_mode int DEFAULT NULL COMMENT '采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造' AFTER subcomp_no,
  ADD COLUMN forest_kind int DEFAULT NULL COMMENT '林种性质 1公益林 2商品林' AFTER cut_mode,
  ADD COLUMN dispute_flag int DEFAULT 0 COMMENT '权属是否在诉 0无 1有' AFTER forest_kind,
  ADD COLUMN area decimal(12,2) DEFAULT NULL COMMENT '本行申报拟采面积(公顷)' AFTER dispute_flag,
  ADD COLUMN survey_flag int DEFAULT 0 COMMENT '附没附伐区调查设计材料 0未附 1已附' AFTER area,
  ADD COLUMN recv_qty decimal(12,2) DEFAULT NULL COMMENT '本行实际核收蓄积(立方米，不得大过申报)' AFTER qty,
  ADD COLUMN period_year int DEFAULT NULL COMMENT '归属执行期年度（进门那年）' AFTER recv_qty,
  ADD COLUMN page_flag int DEFAULT 0 COMMENT '账页 0现行页 1往期页（收满两个执行期转往期）' AFTER period_year,
  ADD COLUMN merge_of_row_no int DEFAULT NULL COMMENT '重复行并账：本行并进的头行行次' AFTER page_flag,
  ADD INDEX idx_declrow_batch_row (batch_no, row_no),
  ADD INDEX idx_declrow_batch_site_sub (batch_no, site_no, subcomp_no),
  ADD INDEX idx_declrow_page (page_flag, period_year),
  ADD INDEX idx_declrow_status (status);

CREATE TABLE IF NOT EXISTS t_flog_declare_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '伐区申报清册批次号',
  site_no varchar(64) DEFAULT NULL COMMENT '申报编制单位底档字号',
  period_year int DEFAULT NULL COMMENT '进门执行期年度',
  total_rows int DEFAULT 0 COMMENT '本册总计行数（原册行次口径，重复行也占行次）',
  accepted_rows int DEFAULT 0 COMMENT '收下几行（逐行落账同一回数出）',
  suspended_rows int DEFAULT 0 COMMENT '挂起几行（逐行落账同一回数出）',
  max_row_no int DEFAULT 0 COMMENT '已进门行次序（最后落定的行次，单行递入时续它）',
  recv_status int DEFAULT 0 COMMENT '收册落法 0收册中 1核讫 2空册 3整册退回',
  recv_time datetime DEFAULT NULL COMMENT '收册回执时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_declbatch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区申报清册批次头与回执';

CREATE TABLE IF NOT EXISTS t_flog_declare_diff (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) NOT NULL COMMENT '批次号',
  site_no varchar(64) DEFAULT NULL COMMENT '编制单位底档字号',
  subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号',
  row_no int DEFAULT NULL COMMENT '原清册行次',
  diff_kind int DEFAULT 1 COMMENT '差异种类 1蓄积调整 2其它更正',
  old_qty decimal(12,2) DEFAULT NULL COMMENT '原申报/原核收蓄积',
  new_qty decimal(12,2) DEFAULT NULL COMMENT '申请改成的蓄积',
  status int DEFAULT 0 COMMENT '办理情形 0待办 1已入帐 2驳回',
  handle_by varchar(64) DEFAULT NULL COMMENT '办理人',
  handle_time datetime DEFAULT NULL COMMENT '办理时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_decldiff_batch (batch_no),
  KEY idx_decldiff_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区申报清册差异清单';

-- 菜单与按钮权限点（清册段），挂给管理员与用户两角色；与 flog.sql 中 seed 同号，已在新库执行过会自动跳过。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000010, '申报清册核收', '伐区申报清册逐行核收', '/FlogDeclareRowController/view', 0, 0,
   'flog:flogDeclareRow:view', 1, 'fa fa-file-text-o', 93, 0, 'seed', NOW()),
  (910000000000000011, '清册逐行查询', '现行页/挂起/往期', '/FlogDeclareRowController/list', 0, 910000000000000010,
   'flog:flogDeclareRow:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000012, '清册收册递行', '整册提交/逐行递入/回执', '/FlogDeclareRowController/submit', 0, 910000000000000010,
   'flog:flogDeclareRow:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000013, '往期结转与差异办理', '收满两期转往期/差异清单入账', '/FlogDeclareRowController/turn', 0, 910000000000000010,
   'flog:flogDeclareRow:edit', 2, '', 3, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000010 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
FROM (
  SELECT 0 AS n, 488243256161730560 AS role_id
  UNION ALL SELECT 1, 488289006124007424
) r
CROSS JOIN (
  SELECT 1 AS n, 910000000000000010 AS permission_id
  UNION ALL SELECT 2, 910000000000000011
  UNION ALL SELECT 3, 910000000000000012
  UNION ALL SELECT 4, 910000000000000013
) p;

-- ============ 2026-09 伐区调查设计·强度界并入系统 ============

-- 旧库此格先允许空着：旧线归类补录（UPDATE 指认 scope_kind）之前不参与取界，
-- 求现时 scope_kind 为空的界一条都请不出来，宁无线可依，不拿错类别的线凑。
ALTER TABLE t_flog_yield_rule
  ADD COLUMN scope_kind int DEFAULT NULL
    COMMENT '管哪一类林地 1商品林 2公益林-抚育 3公益林-更新 4公益林-低质低效林改造' AFTER rule_name,
  ADD INDEX idx_yield_scope_eff (scope_kind, status, del_flag, eff_start, eff_end),
  ADD INDEX idx_yield_rule_code (rule_code);

CREATE TABLE IF NOT EXISTS t_flog_volume_table (
  id bigint NOT NULL COMMENT '主键',
  table_code varchar(64) NOT NULL COMMENT '材积表代号',
  table_ver varchar(32) NOT NULL COMMENT '材积表版本号',
  dbh_min decimal(6,2) NOT NULL COMMENT '胸径档起(厘米，含)',
  dbh_max decimal(6,2) NOT NULL COMMENT '胸径档止(厘米，不含)',
  height_min decimal(6,2) NOT NULL COMMENT '树高档起(米，含)',
  height_max decimal(6,2) NOT NULL COMMENT '树高档止(米，不含)',
  tree_volume decimal(12,6) NOT NULL COMMENT '本档单株材积(立方米)',
  status int DEFAULT 0 COMMENT '情形 0在场可用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_voltab_lookup (table_code, table_ver, status, del_flag, dbh_min, height_min)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='材积表档（胸径+树高折单株材积）';

CREATE TABLE IF NOT EXISTS t_flog_survey_design (
  id bigint NOT NULL COMMENT '主键',
  design_no varchar(64) NOT NULL COMMENT '调查设计单号',
  node_no int NOT NULL COMMENT '本单版本序',
  revise_of_id bigint DEFAULT NULL COMMENT '重录/复验所依上一版id（头版为空）',
  site_no varchar(64) DEFAULT NULL COMMENT '调查编制单位底档字号',
  subcomp_no varchar(64) DEFAULT NULL COMMENT '小班号',
  cut_mode int DEFAULT NULL COMMENT '采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造',
  forest_kind int DEFAULT NULL COMMENT '林种性质 1公益林 2商品林',
  scope_kind int DEFAULT NULL COMMENT '本次定档所管林地类别',
  tree_count int DEFAULT NULL COMMENT '检尺株数',
  area decimal(12,2) DEFAULT NULL COMMENT '小班面积(公顷)',
  yield_rate decimal(7,4) DEFAULT NULL COMMENT '出材率(0..1)',
  volume_code varchar(64) DEFAULT NULL COMMENT '本单所用材积表代号',
  volume_ver varchar(32) DEFAULT NULL COMMENT '本单所钉材积表版本',
  total_volume decimal(14,4) DEFAULT NULL COMMENT '全株逐株累加蓄积(立方米)',
  yield_per_ha decimal(14,4) DEFAULT NULL COMMENT '每公顷出材量(立方米/公顷)',
  grade_level int DEFAULT NULL COMMENT '强度档位 1轻 2中 3重；无线可依为空',
  rule_code varchar(64) DEFAULT NULL COMMENT '定档所依界限值代号（无线可依时写无线可依记号）',
  rule_eff_start datetime DEFAULT NULL COMMENT '所依那条界的启用之日（钉死）',
  rule_basis varchar(255) DEFAULT NULL COMMENT '定档依据一句话（含界上三数，随单留查）',
  survey_date datetime DEFAULT NULL COMMENT '调查设计之日（取界认这一天）',
  survey_by varchar(64) DEFAULT NULL COMMENT '调查员',
  current_flag int DEFAULT 1 COMMENT '版本现行标记 1现行 0历史',
  status int DEFAULT 0 COMMENT '单子情形 0草稿 1已钉 2无线可依',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sdesign_no_node (design_no, node_no),
  KEY idx_sdesign_subcomp (site_no, subcomp_no, current_flag),
  KEY idx_sdesign_survey_date (survey_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区调查设计单（钉结论、钉界代号、钉启用之日）';

CREATE TABLE IF NOT EXISTS t_flog_survey_tree (
  id bigint NOT NULL COMMENT '主键',
  design_id bigint NOT NULL COMMENT '所属设计单版本id',
  design_no varchar(64) NOT NULL COMMENT '调查设计单号',
  node_no int NOT NULL COMMENT '所属版本序',
  tree_seq int NOT NULL COMMENT '株序',
  dbh decimal(6,2) NOT NULL COMMENT '胸径(厘米)',
  tree_height decimal(6,2) NOT NULL COMMENT '树高(米)',
  tree_volume decimal(12,6) DEFAULT NULL COMMENT '本株材积(立方米)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_stree_design (design_id),
  KEY idx_stree_no_node (design_no, node_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='伐区检尺株账（每株胸径树高与折出材积）';

-- 界册种子（与 flog.sql 同号，新库执行过会自动跳过）：商品林秋前换版两版并存；
-- 公益林抚育/更新/低改各一；低改重界空着，空即不设重档，跟着中界往下落。
INSERT IGNORE INTO t_flog_yield_rule
  (id, rule_code, rule_name, scope_kind, th1_max, th2_max, th3_max, eff_start, eff_end, priority, status, del_flag, create_by, create_time)
VALUES
  (10, 'SP-2026A', '商品林强度界（2026 年初版）', 1, 20.00, 40.00, 80.00,
   '2026-01-01 00:00:00', '2026-09-01 00:00:00', 100, 1, 0, 'seed', NOW()),
  (11, 'SP-2026B', '商品林强度界（2026 年秋换版）', 1, 25.00, 50.00, 100.00,
   '2026-09-01 00:00:00', NULL, 100, 0, 0, 'seed', NOW()),
  (12, 'GY-FY-2026', '公益林抚育性质强度界', 2, 15.00, 30.00, 60.00,
   '2026-01-01 00:00:00', NULL, 100, 0, 0, 'seed', NOW()),
  (13, 'GY-GX-2026', '公益林更新性质强度界', 3, 18.00, 35.00, 70.00,
   '2026-01-01 00:00:00', NULL, 100, 0, 0, 'seed', NOW()),
  (14, 'GY-DG-2026', '公益林低质低效林改造性质强度界（重界待立）', 4, 10.00, 20.00, NULL,
   '2026-01-01 00:00:00', NULL, 100, 0, 0, 'seed', NOW());

INSERT IGNORE INTO t_flog_volume_table
  (id, table_code, table_ver, dbh_min, dbh_max, height_min, height_max, tree_volume, status, del_flag, create_by, create_time)
VALUES
  (20, 'VT-ZJ', '2024', 5.00, 14.00, 6.00, 10.00, 0.038000, 0, 0, 'seed', NOW()),
  (21, 'VT-ZJ', '2024', 5.00, 14.00, 10.00, 16.00, 0.056000, 0, 0, 'seed', NOW()),
  (22, 'VT-ZJ', '2024', 14.00, 40.00, 6.00, 10.00, 0.092000, 0, 0, 'seed', NOW()),
  (23, 'VT-ZJ', '2024', 14.00, 40.00, 10.00, 16.00, 0.135000, 0, 0, 'seed', NOW());

-- 界册/材积表/调查设计三段菜单与权限点（与 flog.sql 同号）。
INSERT IGNORE INTO t_sys_permission
  (id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
  (910000000000000014, '强度界限界册', '可采强度界限值线全版册', '/FlogYieldRuleController/view', 0, 0,
   'flog:flogYieldRule:view', 1, 'fa fa-bookmark', 94, 0, 'seed', NOW()),
  (910000000000000015, '界册查询', '现行/让位各版都翻得着来历', '/FlogYieldRuleController/list', 0, 910000000000000014,
   'flog:flogYieldRule:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000016, '界册立线换版', '立新版/旧版交棒，全站交棒日同一口径', '/FlogYieldRuleController/add', 0, 910000000000000014,
   'flog:flogYieldRule:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000017, '材积表册', '胸径树高折材积表档', '/FlogVolumeTableController/view', 0, 0,
   'flog:flogVolumeTable:view', 1, 'fa fa-table', 95, 0, 'seed', NOW()),
  (910000000000000018, '材积表查询', '在场表档查询', '/FlogVolumeTableController/list', 0, 910000000000000017,
   'flog:flogVolumeTable:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000019, '材积表维护', '新表版录入/停旧版', '/FlogVolumeTableController/add', 0, 910000000000000017,
   'flog:flogVolumeTable:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000020, '伐区调查设计', '检尺三级链出单：强度档位与所依界钉死', '/FlogSurveyDesignController/view', 0, 0,
   'flog:flogSurveyDesign:view', 1, 'fa fa-file-text', 96, 0, 'seed', NOW()),
  (910000000000000021, '设计单查询', '现单/旧版回查，抽查同屏同一口径', '/FlogSurveyDesignController/list', 0, 910000000000000020,
   'flog:flogSurveyDesign:list', 2, '', 1, 0, 'seed', NOW()),
  (910000000000000022, '设计单出单', '录检尺、唯一求值口出单并钉结论', '/FlogSurveyDesignController/add', 0, 910000000000000020,
   'flog:flogSurveyDesign:add', 2, '', 2, 0, 'seed', NOW()),
  (910000000000000023, '整改复验重算', '复验重算升版，两回结论各留各的代号', '/FlogSurveyDesignController/edit', 0, 910000000000000020,
   'flog:flogSurveyDesign:edit', 2, '', 3, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
SELECT 911000000000000010 + r.n + 10 * p.n, r.role_id, p.permission_id, 'seed', NOW()
FROM (
  SELECT 0 AS n, 488243256161730560 AS role_id
  UNION ALL SELECT 1, 488289006124007424
) r
CROSS JOIN (
  SELECT 14 AS n, 910000000000000014 AS permission_id
  UNION ALL SELECT 15, 910000000000000015
  UNION ALL SELECT 16, 910000000000000016
  UNION ALL SELECT 17, 910000000000000017
  UNION ALL SELECT 18, 910000000000000018
  UNION ALL SELECT 19, 910000000000000019
  UNION ALL SELECT 20, 910000000000000020
  UNION ALL SELECT 21, 910000000000000021
  UNION ALL SELECT 22, 910000000000000022
  UNION ALL SELECT 23, 910000000000000023
) p;
