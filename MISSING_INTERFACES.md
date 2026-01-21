# 在线考试系统 - 缺失接口清单

## 📋 概述

本文档列出了基于需求分析后，系统中还需要实现的接口。按照业务模块和优先级进行分类。

---

## 🔴 高优先级（核心功能，必须实现）

### 1. 在线考试模块（ExamTakingController）

这是系统的核心功能，目前**完全缺失**。需要实现以下接口：

#### 1.1 考试开始与状态
- **POST** `/api/exam-taking/{examId}/start`
  - 学生开始考试
  - 创建考试记录（ExamRecord），记录开始时间
  - 检查考试权限、时间范围、是否已参加过
  - 返回考试基本信息（总时间、题目数量等）

- **GET** `/api/exam-taking/{examId}/status`
  - 获取当前用户的考试状态
  - 返回：是否已开始、剩余时间、当前进度等

- **GET** `/api/exam-taking/{examId}/continue`
  - 恢复考试（用于断网恢复、刷新页面）
  - 返回：已保存的答案、剩余时间、题目列表（不含答案）

#### 1.2 获取考试题目
- **GET** `/api/exam-taking/{examId}/questions`
  - 获取考试题目列表（不含答案）
  - 返回：题目ID、内容、选项、分值、题型等
  - 包含已保存的答案（用于恢复状态）

#### 1.3 答案保存
- **POST** `/api/exam-taking/{examId}/save-answer`
  - 实时保存单个题目的答案
  - 参数：questionId, answer
  - 自动保存到 ExamAnswer 表
  - 支持批量保存（优化性能）

- **POST** `/api/exam-taking/{examId}/save-answers`
  - 批量保存答案（用于一次性保存多个题目）
  - 参数：List<{questionId, answer}>

#### 1.4 切屏检测
- **POST** `/api/exam-taking/{examId}/cheat-log`
  - 记录切屏行为
  - 参数：cheatType（如：SWITCH_TAB, COPY, PASTE等）
  - 保存到 ExamCheatLog 表
  - 统计切屏次数，可设置阈值警告

- **GET** `/api/exam-taking/{examId}/cheat-logs`
  - 教师查看学生的切屏记录（用于防作弊分析）

#### 1.5 考试提交
- **POST** `/api/exam-taking/{examId}/submit`
  - 提交考试
  - 更新考试记录状态为"已提交"
  - 记录提交时间
  - 触发自动判卷（客观题）
  - 返回：提交成功、自动判卷结果

- **POST** `/api/exam-taking/{examId}/auto-grade`
  - 自动判卷（系统自动调用，也可手动触发）
  - 判客观题（单选题、多选题、判断题、填空题）
  - 更新 ExamAnswer.score 和 ExamRecord.objectiveScore

#### 1.6 时间管理
- **GET** `/api/exam-taking/{examId}/remaining-time`
  - 获取剩余考试时间（秒）
  - 前端用于倒计时显示
  - 时间到了自动提交

---

### 2. 判卷与成绩分析模块（GradingController）

#### 2.1 自动判卷
- **POST** `/api/grading/{examId}/auto-grade`
  - 对指定考试的所有未判卷记录进行自动判卷
  - 判客观题：单选题、多选题、判断题、填空题
  - 主观题（简答题）需要手动阅卷
  - 返回：判卷结果统计

- **POST** `/api/grading/{recordId}/auto-grade`
  - 对单个考试记录进行自动判卷

#### 2.2 手动阅卷
- **GET** `/api/grading/{examId}/subjective-questions`
  - 获取需要手动阅卷的题目列表
  - 返回：待阅卷的简答题列表，包含学生答案

- **POST** `/api/grading/{recordId}/grade-question`
  - 对单个主观题进行评分
  - 参数：questionId, score
  - 更新 ExamAnswer.score
  - 更新 ExamRecord.subjectiveScore 和 totalScore

- **POST** `/api/grading/{recordId}/grade-all`
  - 批量评分（一次给多个主观题评分）
  - 参数：List<{questionId, score}>

#### 2.3 成绩查询（学生端）
- **GET** `/api/grading/my-records`
  - 获取当前用户的考试记录列表
  - 参数：分页、考试ID筛选、状态筛选
  - 返回：考试名称、分数、状态、提交时间等

- **GET** `/api/grading/records/{recordId}`
  - 获取考试记录详情
  - 返回：所有题目的答案、得分、正确答案、错题分析

- **GET** `/api/grading/records/{recordId}/wrong-questions`
  - 获取错题列表
  - 用于学生查看错题和复习

#### 2.4 成绩统计（教师端）
- **GET** `/api/grading/{examId}/statistics`
  - 获取考试的成绩统计
  - 返回：
    - 分数分布（0-60, 60-70, 70-80, 80-90, 90-100的人数）
    - 平均分、最高分、最低分、中位数
    - 通过率（如果设置了及格线）
    - 参与人数、完成人数、未完成人数

- **GET** `/api/grading/{examId}/question-statistics`
  - 获取每道题的统计信息
  - 返回：
    - 题目ID、内容
    - 正确率（正确人数/总人数）
    - 错误答案分布
    - 难度分析

- **GET** `/api/grading/{examId}/students`
  - 获取考试的所有学生成绩列表
  - 参数：分页、分数排序、姓名搜索
  - 返回：学生信息、分数、提交时间、切屏次数等

- **GET** `/api/grading/{examId}/wrong-questions-analysis`
  - 错题分析
  - 返回：每道题的错误人数、错误原因分析

---

## 🟡 中优先级（功能增强）

### 3. 题库管理增强

#### 3.1 批量导入
- **POST** `/api/questions/import`
  - 批量导入题目（Excel/CSV文件）
  - 支持所有题型的导入
  - 返回：导入成功数量、失败数量、错误详情

- **GET** `/api/questions/import-template`
  - 下载导入模板文件

- **POST** `/api/questions/export`
  - 导出题目（Excel/CSV格式）
  - 支持按分类、题型筛选导出

#### 3.2 题目统计
- **GET** `/api/questions/statistics`
  - 获取题库统计信息
  - 返回：各题型数量、各难度数量、各分类数量等

---

### 4. 考试管理增强

#### 4.1 考试记录管理
- **GET** `/api/exams/{examId}/records`
  - 获取考试的所有考试记录
  - 参数：分页、状态筛选、分数排序
  - 返回：学生信息、分数、开始时间、提交时间、状态

- **GET** `/api/exams/{examId}/records/{recordId}`
  - 获取单个考试记录的详情
  - 包含所有答案、得分、切屏记录

#### 4.2 考试权限验证
- **GET** `/api/exams/{examId}/check-permission`
  - 检查当前用户是否可以参加考试
  - 返回：是否允许、不允许的原因

---

### 5. 系统管理增强

#### 5.1 文件上传（用于题目导入、头像上传等）
- **POST** `/api/files/upload`
  - 通用文件上传接口
  - 支持图片、Excel、CSV等
  - 返回：文件URL

---

## 🟢 低优先级（可选功能）

### 6. 消息通知（可选）

#### 6.1 考试通知
- **GET** `/api/notifications`
  - 获取用户的通知列表（新发布的考试等）

### 7. 数据导出（可选）

#### 7.1 成绩导出
- **POST** `/api/grading/{examId}/export`
  - 导出考试成绩（Excel格式）

#### 7.2 考试报告导出
- **POST** `/api/exams/{examId}/report`
  - 生成考试报告（PDF格式）
  - 包含：成绩统计、错题分析、切屏记录等

---

## 📊 接口优先级总结

### 🔴 P0（必须实现，核心功能）
1. **在线考试模块** - 学生参加考试的完整流程
   - 开始考试、获取题目、保存答案、提交考试
   - 切屏检测、时间管理

2. **判卷模块** - 自动判卷和手动阅卷
   - 自动判卷（客观题）
   - 手动阅卷（主观题）

3. **成绩查询** - 学生查看成绩、教师查看统计

### 🟡 P1（重要功能，建议实现）
1. 批量导入题目
2. 成绩统计分析（分数分布、正确率、错题分析）
3. 考试记录管理

### 🟢 P2（可选功能）
1. 文件上传
2. 数据导出
3. 消息通知

---

## 🛠️ 实现建议

### 1. 在线考试模块实现建议

**Service层：ExamTakingService**
```java
public interface ExamTakingService {
    // 开始考试
    Map<String, Object> startExam(Long examId, Long userId);
    
    // 获取考试题目（不含答案）
    List<Map<String, Object>> getExamQuestions(Long examId, Long userId);
    
    // 保存答案
    void saveAnswer(Long examId, Long userId, Long questionId, String answer);
    
    // 批量保存答案
    void saveAnswers(Long examId, Long userId, Map<Long, String> answers);
    
    // 记录切屏
    void logCheat(Long examId, Long userId, String cheatType);
    
    // 提交考试
    Map<String, Object> submitExam(Long examId, Long userId);
    
    // 获取剩余时间
    Long getRemainingTime(Long examId, Long userId);
    
    // 获取考试状态
    Map<String, Object> getExamStatus(Long examId, Long userId);
}
```

### 2. 判卷模块实现建议

**Service层：GradingService**
```java
public interface GradingService {
    // 自动判卷（单个记录）
    Map<String, Object> autoGradeRecord(Long recordId);
    
    // 自动判卷（整个考试）
    Map<String, Object> autoGradeExam(Long examId);
    
    // 手动评分
    void gradeQuestion(Long recordId, Long questionId, BigDecimal score);
    
    // 获取成绩详情
    Map<String, Object> getRecordDetail(Long recordId);
    
    // 获取成绩统计
    Map<String, Object> getExamStatistics(Long examId);
    
    // 获取错题分析
    List<Map<String, Object>> getWrongQuestionsAnalysis(Long examId);
}
```

### 3. 数据一致性考虑

- **实时保存答案**：使用异步保存或批量保存，避免频繁请求
- **并发控制**：使用乐观锁或分布式锁，防止重复提交
- **断网恢复**：保存答案到localStorage，断网恢复后批量同步
- **时间控制**：服务端验证时间，前端倒计时仅作展示

### 4. 安全性考虑

- **防作弊**：切屏检测、禁止复制粘贴、强制全屏
- **权限验证**：每次操作都验证用户权限和考试状态
- **数据验证**：验证答案格式、防止SQL注入等
- **时间校验**：服务端严格校验考试时间，不允许超时提交

---

## 📝 待实现接口详细列表

### ExamTakingController（在线考试控制器）
1. ✅ `POST /api/exam-taking/{examId}/start` - 开始考试
2. ✅ `GET /api/exam-taking/{examId}/status` - 获取考试状态
3. ✅ `GET /api/exam-taking/{examId}/continue` - 恢复考试
4. ✅ `GET /api/exam-taking/{examId}/questions` - 获取题目列表
5. ✅ `POST /api/exam-taking/{examId}/save-answer` - 保存答案
6. ✅ `POST /api/exam-taking/{examId}/save-answers` - 批量保存答案
7. ✅ `POST /api/exam-taking/{examId}/cheat-log` - 记录切屏
8. ✅ `GET /api/exam-taking/{examId}/cheat-logs` - 查看切屏记录
9. ✅ `POST /api/exam-taking/{examId}/submit` - 提交考试
10. ✅ `GET /api/exam-taking/{examId}/remaining-time` - 获取剩余时间

### GradingController（判卷控制器）
1. ✅ `POST /api/grading/{examId}/auto-grade` - 自动判卷（考试）
2. ✅ `POST /api/grading/{recordId}/auto-grade` - 自动判卷（记录）
3. ✅ `GET /api/grading/{examId}/subjective-questions` - 获取待阅卷题目
4. ✅ `POST /api/grading/{recordId}/grade-question` - 手动评分
5. ✅ `POST /api/grading/{recordId}/grade-all` - 批量评分
6. ✅ `GET /api/grading/my-records` - 我的考试记录
7. ✅ `GET /api/grading/records/{recordId}` - 考试记录详情
8. ✅ `GET /api/grading/records/{recordId}/wrong-questions` - 错题列表
9. ✅ `GET /api/grading/{examId}/statistics` - 成绩统计
10. ✅ `GET /api/grading/{examId}/question-statistics` - 题目统计
11. ✅ `GET /api/grading/{examId}/students` - 学生成绩列表
12. ✅ `GET /api/grading/{examId}/wrong-questions-analysis` - 错题分析

### QuestionController 增强
1. ✅ `POST /api/questions/import` - 批量导入
2. ✅ `GET /api/questions/import-template` - 下载模板
3. ✅ `POST /api/questions/export` - 导出题目
4. ✅ `GET /api/questions/statistics` - 题目统计

### ExamController 增强
1. ✅ `GET /api/exams/{examId}/records` - 考试记录列表
2. ✅ `GET /api/exams/{examId}/records/{recordId}` - 记录详情
3. ✅ `GET /api/exams/{examId}/check-permission` - 权限检查

---

**总结**：系统最核心的缺失功能是**在线考试模块**和**判卷与成绩分析模块**，这两个模块的接口是必须实现的，否则系统无法正常使用。
