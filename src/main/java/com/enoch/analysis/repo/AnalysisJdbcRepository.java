package com.enoch.analysis.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AnalysisJdbcRepository {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    AnalysisQueryCatalog queryCatalog;
    private final Map<String, Set<String>> tableColumnsCache = new ConcurrentHashMap<>();


    public Optional<String> findUserDisplayName(long userId) {
        String sql = queryCatalog.get("findUserDisplayName");
        List<String> names = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getString("user_name")
        );
        return names.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst();
    }

    public List<EnrolledTopicRow> findEnrolledTopics(
            long userId,
            Long institutionId,
            Collection<String> excludedQtypes,
            double defaultPassPercentage
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId)
                .addValue("defaultPass", defaultPassPercentage);

        String sqlTemplate = queryCatalog.get("findEnrolledTopicsBase");
        StringBuilder sql = new StringBuilder(sqlTemplate
                .replace("${courseCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "course_code"),
                        new ColumnRef("ss", "syllabus_subject", "course_code"),
                        new ColumnRef("us", "user_syllabus", "course_code")
                )))
                .replace("${sylCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "syl_code"),
                        new ColumnRef("ss", "syllabus_subject", "syl_code"),
                        new ColumnRef("us", "user_syllabus", "syl_code")
                )))
                .replace("${unitCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "unit_code"),
                        new ColumnRef("qt", "questionnaire_template", "unit_name"),
                        new ColumnRef("ss", "syllabus_subject", "unit_code"),
                        new ColumnRef("ss", "syllabus_subject", "unit_name")
                )))
                .replace("${modCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "mod_code"),
                        new ColumnRef("qt", "questionnaire_template", "module_code"),
                        new ColumnRef("qt", "questionnaire_template", "mod_name"),
                        new ColumnRef("qt", "questionnaire_template", "module_name"),
                        new ColumnRef("ss", "syllabus_subject", "mod_code"),
                        new ColumnRef("ss", "syllabus_subject", "module_code")
                )))
        );
        if (institutionId != null) {
            sql.append(" AND us.inst_id = :institutionId");
            params.addValue("institutionId", institutionId);
        }
        appendExcludedQtypeFilter(excludedQtypes, params, sql);
        sql.append("""

                GROUP BY s.id, s.name, qt.topic_code
                ORDER BY s.name, qt.topic_code
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new EnrolledTopicRow(
                        rs.getLong("subject_id"),
                        rs.getString("subject_name"),
                        rs.getString("course_code"),
                        rs.getString("syl_code"),
                        rs.getString("unit_code"),
                        rs.getString("mod_code"),
                        rs.getString("topic_code"),
                        rs.getDouble("required_pass_percentage")
                ));
    }

    public List<AttemptedTopicRow> findAttemptedTopicPerformance(
            long userId,
            Long institutionId,
            Collection<String> excludedQtypes
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        String sqlTemplate = queryCatalog.get("findAttemptedTopicPerformanceBase");
        StringBuilder sql = new StringBuilder(sqlTemplate
                .replace("${courseCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "course_code"),
                        new ColumnRef("ss", "syllabus_subject", "course_code")
                )))
                .replace("${sylCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "syl_code"),
                        new ColumnRef("ss", "syllabus_subject", "syl_code")
                )))
                .replace("${unitCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "unit_code"),
                        new ColumnRef("qt", "questionnaire_template", "unit_name"),
                        new ColumnRef("ss", "syllabus_subject", "unit_code"),
                        new ColumnRef("ss", "syllabus_subject", "unit_name")
                )))
                .replace("${modCodeExpr}", resolveExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "mod_code"),
                        new ColumnRef("qt", "questionnaire_template", "module_code"),
                        new ColumnRef("qt", "questionnaire_template", "mod_name"),
                        new ColumnRef("qt", "questionnaire_template", "module_name"),
                        new ColumnRef("ss", "syllabus_subject", "mod_code"),
                        new ColumnRef("ss", "syllabus_subject", "module_code")
                )))
        );
        appendInstitutionFilter(institutionId, params, sql, "qt.institution_id");
        appendExcludedQtypeFilter(excludedQtypes, params, sql);
        sql.append("""

                GROUP BY s.id, s.name, qt.topic_code
                ORDER BY s.name, qt.topic_code
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new AttemptedTopicRow(
                        rs.getLong("subject_id"),
                        rs.getString("subject_name"),
                        rs.getString("course_code"),
                        rs.getString("syl_code"),
                        rs.getString("unit_code"),
                        rs.getString("mod_code"),
                        rs.getString("topic_code"),
                        rs.getLong("attempts_count"),
                        rs.getDouble("average_score_percentage"),
                        rs.getDouble("highest_score_percentage"),
                        rs.getTimestamp("last_submitted_date")
                ));
    }

    public List<TopicQuestionProgressRow> findTopicQuestionProgress(
            long userId,
            Long institutionId,
            Collection<String> excludedQtypes
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        String sqlTemplate = queryCatalog.get("findTopicQuestionProgressBase");
        StringBuilder sql = new StringBuilder(sqlTemplate
                .replace("${topicQuestionCountJoin}", buildTopicQuestionCountJoin())
                .replace("${templateQuestionCountExpr}", resolveNumericExpression(List.of(
                        new ColumnRef("qt", "questionnaire_template", "question_count"),
                        new ColumnRef("qt", "questionnaire_template", "questions_count"),
                        new ColumnRef("qt", "questionnaire_template", "total_questions"),
                        new ColumnRef("qt", "questionnaire_template", "total_question"),
                        new ColumnRef("qt", "questionnaire_template", "no_of_questions"),
                        new ColumnRef("qt", "questionnaire_template", "no_of_question"),
                        new ColumnRef("qt", "questionnaire_template", "question_cnt"),
                        new ColumnRef("qt", "questionnaire_template", "no_of_qstns")
                )))
        );
        if (institutionId != null) {
            sql.append(" AND us.inst_id = :institutionId");
            params.addValue("institutionId", institutionId);
        }
        appendExcludedQtypeFilter(excludedQtypes, params, sql);
        sql.append("""

                GROUP BY s.id, qt.topic_code
                ORDER BY s.id, qt.topic_code
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new TopicQuestionProgressRow(
                        rs.getLong("subject_id"),
                        rs.getString("topic_code"),
                        rs.getLong("questions_count"),
                        rs.getLong("attended_questions_count")
                ));
    }

    public List<TopicAttemptScoreRow> findTopicAttemptScores(
            long userId,
            Long institutionId,
            Collection<String> excludedQtypes
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        StringBuilder sql = new StringBuilder(queryCatalog.get("findTopicAttemptScoresBase"));
        appendInstitutionFilter(institutionId, params, sql, "qt.institution_id");
        appendExcludedQtypeFilter(excludedQtypes, params, sql);
        sql.append("""

                GROUP BY s.id, qt.topic_code, qp.id, qp.create_date
                ORDER BY s.id, qt.topic_code, qp.create_date, qp.id
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new TopicAttemptScoreRow(
                        rs.getLong("subject_id"),
                        rs.getString("topic_code"),
                        rs.getLong("paper_id"),
                        rs.getTimestamp("created_at"),
                        rs.getDouble("attempt_percentage")
                ));
    }

    public List<TopicCognitiveStatsRow> findTopicCognitiveStats(
            long userId,
            Long institutionId,
            Collection<String> excludedQtypes
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId)
                .addValue("fastIncorrectThresholdMs", 8000L);

        String sqlTemplate = queryCatalog.get("findTopicCognitiveStatsBase");
        StringBuilder sql = new StringBuilder(sqlTemplate
                .replace("${timeTakenExpr}", resolveTimeTakenExpression("qpa"))
                .replace("${difficultyWeightExpr}", resolveDifficultyWeightExpression())
        );
        if (institutionId != null) {
            sql.append(" AND us.inst_id = :institutionId");
            params.addValue("institutionId", institutionId);
        }
        appendExcludedQtypeFilter(excludedQtypes, params, sql);
        sql.append("""

                GROUP BY s.id, qt.topic_code
                ORDER BY s.id, qt.topic_code
                """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new TopicCognitiveStatsRow(
                        rs.getLong("subject_id"),
                        rs.getString("topic_code"),
                        rs.getDouble("weighted_correct_sum"),
                        rs.getDouble("weighted_attempt_sum"),
                        rs.getLong("fast_incorrect_count"),
                        rs.getLong("total_answer_attempts"),
                        rs.getLong("total_time_taken_ms"),
                        rs.getLong("reattempt_count"),
                        rs.getLong("correct_reattempt_count")
                ));
    }

    private void appendInstitutionFilter(
            Long institutionId,
            MapSqlParameterSource params,
            StringBuilder sql,
            String institutionColumn
    ) {
        if (institutionId != null) {
            sql.append(" AND ").append(institutionColumn).append(" = :institutionId");
            params.addValue("institutionId", institutionId);
        }
    }

    private void appendExcludedQtypeFilter(
            Collection<String> excludedQtypes,
            MapSqlParameterSource params,
            StringBuilder sql
    ) {
        List<String> normalized = normalizeQtypes(excludedQtypes);
        sql.append(" AND UPPER(COALESCE(qt.qtype, '')) NOT IN (:excludedQtypes)");
        params.addValue("excludedQtypes", normalized);
    }

    private List<String> normalizeQtypes(Collection<String> qtypes) {
        if (qtypes == null || qtypes.isEmpty()) {
            return Collections.singletonList("__NONE__");
        }
        List<String> normalized = new ArrayList<>();
        for (String qtype : qtypes) {
            if (StringUtils.hasText(qtype)) {
                normalized.add(qtype.trim().toUpperCase());
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("__NONE__");
        }
        return normalized;
    }

    private String buildTopicQuestionCountJoin() {
        Optional<String> templateTopicCode = resolveFirstExistingColumn("questionnaire_template", List.of(
                "topic_code",
                "topic",
                "topic_name",
                "topiccode",
                "tpc_code",
                "code"
        ));
        Optional<String> topicQuestionQuestionId = resolveFirstExistingColumn("topic_question", List.of(
                "que_id",
                "question_id",
                "qstn_id",
                "ques_id",
                "questionid",
                "qstnid",
                "q_id"
        ));
        Optional<String> topicQuestionModTopId = resolveFirstExistingColumn("topic_question", List.of(
                "mod_top_id",
                "module_topic_id",
                "modtopic_id",
                "mod_topic_id",
                "modtopid"
        ));
        Optional<String> moduleTopicId = resolveFirstExistingColumn("module_topic", List.of(
                "id",
                "module_topic_id",
                "mod_top_id",
                "modtopic_id"
        ));
        Optional<String> moduleTopicTopicId = resolveFirstExistingColumn("module_topic", List.of(
                "topic_id",
                "tpc_id",
                "topicid",
                "topic_ref_id",
                "topic_fk"
        ));
        Optional<String> topicId = resolveFirstExistingColumn("topic", List.of(
                "id",
                "topic_id",
                "topicid",
                "tpc_id"
        ));
        String topicTextExpr = resolveTopicTextExpression("t");
        if (templateTopicCode.isEmpty()
                || topicQuestionQuestionId.isEmpty()
                || topicQuestionModTopId.isEmpty()
                || moduleTopicId.isEmpty()
                || moduleTopicTopicId.isEmpty()
                || topicId.isEmpty()
                || topicTextExpr.equals("''")) {
            return "LEFT JOIN (SELECT '' AS topic_key, 0 AS topic_questions_count) tqc ON 1=0";
        }

        String topicKeyExpr = normalizedExpression(topicTextExpr);
        String derived = "SELECT " + topicKeyExpr + " AS topic_key, COUNT(DISTINCT tq." + topicQuestionQuestionId.get() + ") AS topic_questions_count " +
                "FROM topic_question tq " +
                "JOIN module_topic mt ON mt." + moduleTopicId.get() + " = tq." + topicQuestionModTopId.get() + " " +
                "JOIN topic t ON t." + topicId.get() + " = mt." + moduleTopicTopicId.get() + " " +
                "WHERE tq." + topicQuestionQuestionId.get() + " IS NOT NULL " +
                "GROUP BY " + topicKeyExpr;
        return "LEFT JOIN (" + derived + ") tqc ON tqc.topic_key = " + normalizedToken("qt", templateTopicCode.get());
    }

    private String buildTopicQuestionCountExpression() {
        if (!hasTable("topic_question")) {
            return "0";
        }
        Optional<String> topicQuestionIdColumn = resolveFirstExistingColumn("topic_question", List.of(
                "question_id",
                "qstn_id",
                "ques_id",
                "que_id",
                "questionid",
                "qstnid",
                "q_id",
                "questionnaire_question_id",
                "qq_id",
                "question_bank_id",
                "qbank_id",
                "qus_id"
        ));
        Optional<String> topicMatchCondition = resolveTopicQuestionMatchCondition("tq", "qt");
        if (topicMatchCondition.isEmpty()) {
            return "0";
        }
        String countExpr = topicQuestionIdColumn
                .map(column -> "COUNT(DISTINCT tq." + column + ")")
                .orElse("COUNT(*)");
        String notNullFilter = topicQuestionIdColumn
                .map(column -> " AND tq." + column + " IS NOT NULL")
                .orElse("");
        return "(SELECT " + countExpr + " FROM topic_question tq WHERE " + topicMatchCondition.get() + notNullFilter + ")";
    }

    private String buildTopicAttendedQuestionCountExpression() {
        if (!hasTable("topic_question")) {
            return "0";
        }
        Optional<String> topicQuestionIdColumn = resolveFirstExistingColumn("topic_question", List.of(
                "question_id",
                "qstn_id",
                "ques_id",
                "que_id",
                "questionid",
                "qstnid",
                "q_id",
                "questionnaire_question_id",
                "qq_id",
                "question_bank_id",
                "qbank_id",
                "qus_id"
        ));
        Optional<String> answerQuestionIdColumn = resolveFirstExistingColumn("question_paper_answer", List.of(
                "question_id",
                "qstn_id",
                "ques_id",
                "questionid",
                "qstnid",
                "qq_id",
                "questionnaire_question_id"
        ));
        Optional<String> answerPaperIdColumn = resolveFirstExistingColumn("question_paper_answer", List.of(
                "paper_id",
                "qp_id",
                "paperid"
        ));
        Optional<String> topicMatchCondition = resolveTopicQuestionMatchCondition("tq", "qt");
        if (topicQuestionIdColumn.isEmpty()
                || answerQuestionIdColumn.isEmpty()
                || answerPaperIdColumn.isEmpty()
                || topicMatchCondition.isEmpty()) {
            return "0";
        }
        return "(SELECT COUNT(DISTINCT qpa." + answerQuestionIdColumn.get() + ") " +
                "FROM question_paper qp " +
                "JOIN question_paper_answer qpa ON qpa." + answerPaperIdColumn.get() + " = qp.id " +
                "JOIN topic_question tq ON tq." + topicQuestionIdColumn.get() + " = qpa." + answerQuestionIdColumn.get() + " " +
                "WHERE qp.user_id = :userId " +
                "AND " + topicMatchCondition.get() + ")";
    }

    private Optional<String> resolveTopicQuestionMatchCondition(String topicQuestionAlias, String templateAlias) {
        Set<String> conditions = new LinkedHashSet<>();

        Optional<String> modTopCondition = resolveModuleTopicPathCondition(topicQuestionAlias, templateAlias);
        modTopCondition.ifPresent(conditions::add);

        Optional<String> topicQuestionTopicId = resolveFirstExistingColumn("topic_question", List.of(
                "topic_id",
                "tpc_id",
                "topicid",
                "tpcid",
                "topic_ref_id",
                "topic_fk"
        ));
        Optional<String> templateTopicId = resolveFirstExistingColumn("questionnaire_template", List.of(
                "topic_id",
                "tpc_id",
                "topicid",
                "tpcid",
                "topic_ref_id",
                "topic_fk"
        ));
        if (topicQuestionTopicId.isPresent() && templateTopicId.isPresent()) {
            conditions.add("(" + templateAlias + "." + templateTopicId.get() + " IS NOT NULL AND "
                    + topicQuestionAlias + "." + topicQuestionTopicId.get()
                    + " = " + templateAlias + "." + templateTopicId.get() + ")");
        }

        Optional<String> topicQuestionTopicCode = resolveFirstExistingColumn("topic_question", List.of(
                "topic_code",
                "topic",
                "topic_name",
                "tpc_code",
                "code",
                "topiccode",
                "tpc",
                "topic_title"
        ));
        Optional<String> templateTopicCode = resolveFirstExistingColumn("questionnaire_template", List.of(
                "topic_code",
                "topic",
                "topic_name",
                "tpc_code",
                "code",
                "topiccode",
                "tpc",
                "topic_title"
        ));
        if (topicQuestionTopicCode.isPresent() && templateTopicCode.isPresent()) {
            conditions.add(normalizedEquals(
                    topicQuestionAlias,
                    topicQuestionTopicCode.get(),
                    templateAlias,
                    templateTopicCode.get()
            ));
        }

        Optional<String> topicTableId = resolveFirstExistingColumn("topic", List.of(
                "id",
                "topic_id",
                "tpc_id",
                "topicid",
                "tpcid"
        ));
        Optional<String> topicTableCode = resolveFirstExistingColumn("topic", List.of(
                "topic_code",
                "code",
                "topic_name",
                "name",
                "topic",
                "topiccode",
                "tpc_code",
                "title"
        ));
        if (topicQuestionTopicId.isPresent() && topicTableId.isPresent() && topicTableCode.isPresent() && templateTopicCode.isPresent()) {
            conditions.add(
                    "EXISTS (SELECT 1 FROM topic t WHERE t." + topicTableId.get()
                            + " = " + topicQuestionAlias + "." + topicQuestionTopicId.get()
                            + " AND " + normalizedEquals("t", topicTableCode.get(), templateAlias, templateTopicCode.get())
                            + ")"
            );
        }

        if (conditions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("(" + String.join(" OR ", conditions) + ")");
    }

    private Optional<String> resolveModuleTopicPathCondition(String topicQuestionAlias, String templateAlias) {
        Optional<String> topicQuestionModTopId = resolveFirstExistingColumn("topic_question", List.of(
                "mod_top_id",
                "module_topic_id",
                "modtopic_id",
                "mod_topic_id",
                "modtopid"
        ));
        Optional<String> moduleTopicId = resolveFirstExistingColumn("module_topic", List.of(
                "id",
                "module_topic_id",
                "mod_top_id",
                "modtopic_id"
        ));
        Optional<String> moduleTopicTopicId = resolveFirstExistingColumn("module_topic", List.of(
                "topic_id",
                "tpc_id",
                "topicid",
                "topic_ref_id",
                "topic_fk"
        ));
        Optional<String> topicTableId = resolveFirstExistingColumn("topic", List.of(
                "id",
                "topic_id",
                "tpc_id",
                "topicid",
                "tpcid"
        ));
        Optional<String> templateTopicCode = resolveFirstExistingColumn("questionnaire_template", List.of(
                "topic_code",
                "topic",
                "topic_name",
                "tpc_code",
                "code",
                "topiccode",
                "tpc",
                "topic_title"
        ));
        List<String> topicTextColumns = resolveExistingColumns("topic", List.of(
                "name",
                "topic_name",
                "topic_code",
                "code",
                "title",
                "shrt_cd",
                "lng_cd"
        ));
        if (topicQuestionModTopId.isEmpty()
                || moduleTopicId.isEmpty()
                || moduleTopicTopicId.isEmpty()
                || topicTableId.isEmpty()
                || templateTopicCode.isEmpty()
                || topicTextColumns.isEmpty()) {
            return Optional.empty();
        }
        List<String> textMatches = new ArrayList<>();
        for (String topicTextColumn : topicTextColumns) {
            textMatches.add(normalizedEquals("t", topicTextColumn, templateAlias, templateTopicCode.get()));
        }
        return Optional.of(
                "EXISTS (SELECT 1 FROM module_topic mt " +
                        "JOIN topic t ON t." + topicTableId.get() + " = mt." + moduleTopicTopicId.get() + " " +
                        "WHERE mt." + moduleTopicId.get() + " = " + topicQuestionAlias + "." + topicQuestionModTopId.get() +
                        " AND (" + String.join(" OR ", textMatches) + "))"
        );
    }

    private Optional<String> resolveFirstExistingColumn(String tableName, List<String> candidates) {
        for (String candidate : candidates) {
            if (hasColumn(tableName, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private List<String> resolveExistingColumns(String tableName, List<String> candidates) {
        List<String> presentColumns = new ArrayList<>();
        for (String candidate : candidates) {
            if (hasColumn(tableName, candidate)) {
                presentColumns.add(candidate);
            }
        }
        return presentColumns;
    }

    private String resolveExpression(List<ColumnRef> candidates) {
        for (ColumnRef candidate : candidates) {
            if (hasColumn(candidate.tableName(), candidate.columnName())) {
                return "MAX(NULLIF(TRIM(" + candidate.alias() + "." + candidate.columnName() + "), ''))";
            }
        }
        return "''";
    }

    private String resolveNumericExpression(List<ColumnRef> candidates) {
        for (ColumnRef candidate : candidates) {
            if (hasColumn(candidate.tableName(), candidate.columnName())) {
                return "COALESCE(" + candidate.alias() + "." + candidate.columnName() + ", 0)";
            }
        }
        return "0";
    }

    private String resolveTimeTakenExpression(String answerAlias) {
        List<String> timeColumns = List.of(
                "time_taken",
                "timetaken",
                "timeTaken",
                "time_spent",
                "duration_ms",
                "timems",
                "time"
        );
        for (String column : timeColumns) {
            if (hasColumn("question_paper_answer", column)) {
                return "COALESCE(" + answerAlias + "." + column + ", 0)";
            }
        }
        return "0";
    }

    private String resolveDifficultyWeightExpression() {
        for (String difficultyColumn : List.of("difficulty", "difficulty_level", "level", "difficultyfactor")) {
            if (hasColumn("question", difficultyColumn)) {
                return "CASE " +
                        "WHEN UPPER(TRIM(COALESCE(qu." + difficultyColumn + ", ''))) IN ('EASY', '1', 'LOW') THEN 1.0 " +
                        "WHEN UPPER(TRIM(COALESCE(qu." + difficultyColumn + ", ''))) IN ('MEDIUM', '2', 'MID') THEN 1.5 " +
                        "WHEN UPPER(TRIM(COALESCE(qu." + difficultyColumn + ", ''))) IN ('HARD', '3', 'HIGH') THEN 2.0 " +
                        "ELSE 1.0 END";
            }
        }
        if (hasColumn("question", "probable")) {
            return "CASE " +
                    "WHEN COALESCE(qu.probable, 0) >= 7 THEN 1.0 " +
                    "WHEN COALESCE(qu.probable, 0) >= 4 THEN 1.5 " +
                    "WHEN COALESCE(qu.probable, 0) > 0 THEN 2.0 " +
                    "ELSE 1.0 END";
        }
        return "1.0";
    }

    private String resolveTopicTextExpression(String topicAlias) {
        List<String> topicTextColumns = resolveExistingColumns("topic", List.of(
                "name",
                "topic_name",
                "topic_code",
                "code",
                "title",
                "shrt_cd",
                "lng_cd"
        ));
        if (topicTextColumns.isEmpty()) {
            return "''";
        }
        if (topicTextColumns.size() == 1) {
            return topicAlias + "." + topicTextColumns.get(0);
        }
        List<String> pieces = new ArrayList<>();
        for (String column : topicTextColumns) {
            pieces.add("NULLIF(TRIM(" + topicAlias + "." + column + "), '')");
        }
        return "COALESCE(" + String.join(", ", pieces) + ", '')";
    }

    private String normalizedExpression(String rawExpression) {
        return "UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(" + rawExpression + ", '')), ' ', ''), '-', ''), '_', ''))";
    }

    private String normalizedEquals(String leftAlias, String leftColumn, String rightAlias, String rightColumn) {
        return normalizedToken(leftAlias, leftColumn) + " = " + normalizedToken(rightAlias, rightColumn);
    }

    private String normalizedToken(String alias, String column) {
        return "UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(" + alias + "." + column + ", '')), ' ', ''), '-', ''), '_', ''))";
    }

    private boolean hasColumn(String tableName, String columnName) {
        Set<String> columns = tableColumnsCache.computeIfAbsent(tableName, this::loadTableColumns);
        return columns.contains(columnName.toLowerCase());
    }

    private boolean hasTable(String tableName) {
        Set<String> columns = tableColumnsCache.computeIfAbsent(tableName, this::loadTableColumns);
        return !columns.isEmpty();
    }

    private Set<String> loadTableColumns(String tableName) {
        try {
            String sql = """
                    SELECT LOWER(COLUMN_NAME) AS column_name
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = :tableName
                    """;
            List<String> rows = jdbcTemplate.query(
                    sql,
                    new MapSqlParameterSource("tableName", tableName),
                    (rs, rowNum) -> rs.getString("column_name")
            );
            return new HashSet<>(rows);
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    public record EnrolledTopicRow(
            long subjectId,
            String subjectName,
            String courseCode,
            String sylCode,
            String unitCode,
            String modCode,
            String topicCode,
            double requiredPassPercentage
    ) {
    }

    public record AttemptedTopicRow(
            long subjectId,
            String subjectName,
            String courseCode,
            String sylCode,
            String unitCode,
            String modCode,
            String topicCode,
            long attemptsCount,
            double averageScorePercentage,
            double highestScorePercentage,
            Timestamp lastSubmittedDate
    ) {
    }

    public record TopicQuestionProgressRow(
            long subjectId,
            String topicCode,
            long questionsCount,
            long attendedQuestionsCount
    ) {
    }

    public record TopicAttemptScoreRow(
            long subjectId,
            String topicCode,
            long paperId,
            Timestamp createdAt,
            double attemptPercentage
    ) {
    }

    public record TopicCognitiveStatsRow(
            long subjectId,
            String topicCode,
            double weightedCorrectSum,
            double weightedAttemptSum,
            long fastIncorrectCount,
            long totalAnswerAttempts,
            long totalTimeTakenMs,
            long reattemptCount,
            long correctReattemptCount
    ) {
    }

    private record ColumnRef(String alias, String tableName, String columnName) {
    }
}
