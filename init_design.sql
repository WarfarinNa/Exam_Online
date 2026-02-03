
INSERT INTO permission (permission_code, permission_name, description) VALUES
-- 用户管理
('user:manage', '用户管理', '管理员管理用户信息与角色'),
('user:profile', '个人信息维护', '修改个人信息与密码'),

-- 题库管理
('question:manage', '题库管理', '新增、编辑、删除、查看题目'),

-- 试卷管理
('paper:manage', '试卷管理', '创建、发布、查看试卷'),

-- 考试管理
('exam:manage', '考试管理', '创建考试、发布考试、查看考试状态与参与情况'),
('exam:participate', '参加考试', '学生参加考试'),

-- 判卷与成绩
('mark:auto', '自动判卷', '系统自动判分'),
('mark:manual', '人工阅卷', '教师批改主观题'),
('score:view', '查看成绩', '学生或教师查看考试成绩与分析'),

-- 系统配置
('system:config', '系统配置', '系统全局配置管理');

INSERT INTO role(id,role_name) VALUES
(1,'ADMIN'),
(2,'TEACHER'),
(3,'STUDENT');

-- 管理员：全部权限
INSERT INTO role_permission(role_id, permission_id)
SELECT 1, id FROM permission;

-- 教师：题库、试卷、考试管理、人工阅卷
INSERT INTO role_permission(role_id, permission_id)
SELECT 2, id FROM permission
WHERE permission_code IN (
'question:manage',
'paper:manage',
'exam:manage',
'mark:manual',
'score:view'
);

-- 学生：参加考试 + 查看成绩 + 个人信息维护
INSERT INTO role_permission(role_id, permission_id)
SELECT 3, id FROM permission
WHERE permission_code IN (
'exam:participate',
'score:view',
'user:profile'
);
