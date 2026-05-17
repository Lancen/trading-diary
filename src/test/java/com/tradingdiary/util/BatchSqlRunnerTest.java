package com.tradingdiary.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@org.springframework.test.context.TestPropertySource(properties = {"spring.flyway.enabled=false"})
class BatchSqlRunnerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BatchSqlRunner runner;

    @BeforeEach
    void setUp() {
        runner = new BatchSqlRunner(jdbcTemplate);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_entity (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100),
                amount DECIMAL(20,4),
                trade_date DATE,
                is_deleted TINYINT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbcTemplate.execute("DELETE FROM test_entity");
    }

    @Test
    void batchInsertSingle() {
        TestEntity e = new TestEntity();
        e.setName("single");
        e.setAmount(new BigDecimal("99.50"));
        e.setTradeDate(LocalDate.of(2026, 5, 18));

        int rows = runner.batchInsert(List.of(e));
        assertThat(rows).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM test_entity WHERE name = 'single'");
        assertThat(row.get("name")).isEqualTo("single");
        assertThat(row.get("amount")).isEqualTo(new BigDecimal("99.5000"));
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void batchInsertMultiple() {
        List<TestEntity> entities = List.of(
                makeEntity("a", 10, LocalDate.of(2026, 5, 18)),
                makeEntity("b", 20, LocalDate.of(2026, 5, 19)),
                makeEntity("c", 30, LocalDate.of(2026, 5, 20))
        );

        int rows = runner.batchInsert(entities);
        assertThat(rows).isEqualTo(3);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void batchInsertEmpty() {
        int rows = runner.batchInsert(List.of());
        assertThat(rows).isEqualTo(0);
    }

    @Test
    void batchUpdateSingle() {
        TestEntity e = makeEntity("old", 100, LocalDate.of(2026, 5, 18));
        runner.batchInsert(List.of(e));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM test_entity WHERE name = 'old'", Long.class);

        TestEntity update = new TestEntity();
        update.setId(id);
        update.setName("new");
        update.setAmount(new BigDecimal("200"));
        update.setTradeDate(LocalDate.of(2026, 5, 19));

        int rows = runner.batchUpdate(List.of(update));
        assertThat(rows).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM test_entity WHERE id = ?", id);
        assertThat(row.get("name")).isEqualTo("new");
        assertThat(row.get("amount")).isEqualTo(new BigDecimal("200.0000"));
    }

    @Test
    void batchUpdateMultiple() {
        TestEntity e1 = makeEntity("e1", 10, LocalDate.of(2026, 5, 18));
        TestEntity e2 = makeEntity("e2", 20, LocalDate.of(2026, 5, 18));
        TestEntity e3 = makeEntity("e3", 30, LocalDate.of(2026, 5, 18));
        runner.batchInsert(List.of(e1, e2, e3));

        List<Map<String, Object>> inserted = jdbcTemplate.queryForList("SELECT id FROM test_entity ORDER BY name");
        Long id1 = (Long) inserted.get(0).get("id");
        Long id2 = (Long) inserted.get(1).get("id");
        Long id3 = (Long) inserted.get(2).get("id");

        TestEntity u1 = new TestEntity();
        u1.setId(id1);
        u1.setName("u1");
        u1.setAmount(new BigDecimal("111"));
        u1.setTradeDate(LocalDate.of(2026, 6, 1));

        TestEntity u2 = new TestEntity();
        u2.setId(id2);
        u2.setName("u2");
        u2.setAmount(new BigDecimal("222"));
        u2.setTradeDate(LocalDate.of(2026, 6, 2));

        TestEntity u3 = new TestEntity();
        u3.setId(id3);
        u3.setName("u3");
        u3.setAmount(new BigDecimal("333"));
        u3.setTradeDate(LocalDate.of(2026, 6, 3));

        int rows = runner.batchUpdate(List.of(u1, u2, u3));
        assertThat(rows).isEqualTo(3);

        Map<String, Object> r1 = jdbcTemplate.queryForMap("SELECT * FROM test_entity WHERE id = ?", id1);
        assertThat(r1.get("name")).isEqualTo("u1");
        assertThat(r1.get("amount")).isEqualTo(new BigDecimal("111.0000"));

        Map<String, Object> r3 = jdbcTemplate.queryForMap("SELECT * FROM test_entity WHERE id = ?", id3);
        assertThat(r3.get("name")).isEqualTo("u3");
        assertThat(r3.get("amount")).isEqualTo(new BigDecimal("333.0000"));
    }

    @Test
    void camelToSnake() {
        assertThat(runner.camelToSnake("stockCode")).isEqualTo("stock_code");
        assertThat(runner.camelToSnake("isDeleted")).isEqualTo("is_deleted");
        assertThat(runner.camelToSnake("pe")).isEqualTo("pe");
        assertThat(runner.camelToSnake("tradeDate")).isEqualTo("trade_date");
        assertThat(runner.camelToSnake("marginBuy")).isEqualTo("margin_buy");
    }

    @Test
    void toJdbcValue() {
        assertThat(runner.toJdbcValue(null)).isNull();
        assertThat(runner.toJdbcValue(LocalDate.of(2026, 5, 18))).isInstanceOf(java.sql.Date.class);
        assertThat(runner.toJdbcValue(LocalDateTime.now())).isInstanceOf(java.sql.Timestamp.class);
        assertThat(runner.toJdbcValue("string")).isEqualTo("string");
        assertThat(runner.toJdbcValue(new BigDecimal("1.23"))).isEqualTo(new BigDecimal("1.23"));
        assertThat(runner.toJdbcValue(100L)).isEqualTo(100L);
        assertThat(runner.toJdbcValue(true)).isEqualTo(1);
        assertThat(runner.toJdbcValue(false)).isEqualTo(0);
    }

    @Test
    void batchInsertExceedsBatchSize() {
        List<TestEntity> entities = new java.util.ArrayList<>();
        for (int i = 0; i < 550; i++) {
            entities.add(makeEntity("item" + i, i, LocalDate.of(2026, 5, 18)));
        }

        int rows = runner.batchInsert(entities, 100);
        assertThat(rows).isEqualTo(550);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
        assertThat(count).isEqualTo(550);
    }

    @Test
    void batchUpdateExceedsBatchSize() {
        List<TestEntity> entities = new java.util.ArrayList<>();
        for (int i = 0; i < 250; i++) {
            entities.add(makeEntity("item" + i, i, LocalDate.of(2026, 5, 18)));
        }
        runner.batchInsert(entities, 100);

        List<Map<String, Object>> inserted = jdbcTemplate.queryForList("SELECT id FROM test_entity ORDER BY id");
        List<TestEntity> updates = new java.util.ArrayList<>();
        for (int i = 0; i < inserted.size(); i++) {
            TestEntity u = new TestEntity();
            u.setId((Long) inserted.get(i).get("id"));
            u.setName("updated" + i);
            u.setAmount(new BigDecimal(i * 10));
            u.setTradeDate(LocalDate.of(2026, 6, 15));
            updates.add(u);
        }

        int rows = runner.batchUpdate(updates, 100);
        assertThat(rows).isEqualTo(250);
    }

    @Test
    void buildMetaShouldRejectMissingTableName() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                BatchSqlRunner.buildMeta(MissingTableName.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@TableName");
    }

    @Test
    void buildMetaShouldRejectMissingId() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                BatchSqlRunner.buildMeta(MissingId.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@TableId");
    }

    @Test
    void buildInsertSql() {
        BatchSqlRunner.EntityMeta meta = BatchSqlRunner.buildMeta(TestEntity.class);
        String sql = runner.buildInsertSql(meta, 2);
        assertThat(sql).contains("INSERT INTO test_entity");
        assertThat(sql).contains("VALUES (?, ?, ?, ?");
        assertThat(sql).contains("created_at");
        assertThat(sql).contains("updated_at");
    }

    @Test
    void buildUpdateSql() {
        BatchSqlRunner.EntityMeta meta = BatchSqlRunner.buildMeta(TestEntity.class);
        String sql = runner.buildUpdateSql(meta, 2);
        assertThat(sql).contains("UPDATE test_entity SET");
        assertThat(sql).contains("CASE id");
        assertThat(sql).contains("WHEN ? THEN ?");
        assertThat(sql).contains("updated_at = ?");
        assertThat(sql).contains("WHERE id IN (?, ?)");
    }

    private TestEntity makeEntity(String name, double amount, LocalDate date) {
        TestEntity e = new TestEntity();
        e.setName(name);
        e.setAmount(BigDecimal.valueOf(amount));
        e.setTradeDate(date);
        return e;
    }

    @TableName("test_entity")
    static class TestEntity {
        @TableId(type = IdType.AUTO)
        private Long id;

        private String name;

        private BigDecimal amount;

        private LocalDate tradeDate;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        @SuppressWarnings("unused")
        private Boolean isDeleted = false;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getTradeDate() { return tradeDate; }
        public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public Boolean getIsDeleted() { return isDeleted; }
        public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    }

    @TableName("missing_id")
    static class MissingId {
        private String name;
    }

    static class MissingTableName {
        @TableId(type = IdType.AUTO)
        private Long id;
    }
}
