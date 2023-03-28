package com.lixin.etl.db.table;


import com.lixin.etl.db.model.ColumnStatus;
import com.lixin.etl.db.model.MysqlColumn;
import io.micrometer.common.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Description:
 * Copyright:   Copyright (c)2023
 * Company:     sci
 *
 * @author: 张李鑫
 * @version: 1.0
 * Create at:   2023-03-16 16:38:53
 * <p>
 * Modification History:
 * Date         Author      Version     Description
 * ------------------------------------------------------------------
 * 2023-03-16     张李鑫                     1.0         1.0 Version
 */
public class MysqlTable extends Table {



    private static final String FORMAT = " `%s` ";
    private static final String LINE_FEED_HAS_NEXT = ",\n";
    private static final String LINE_FEED_NOT_HAS_NEXT = "\n";
    private static final String TIMESTAMP_DEFAULT = " DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ";
    private static final String PRIMARY_KEY_STR = "  PRIMARY KEY (`%s`)";
    private static final String MODIFY = "  modify ";
    private static final String COMMENT = "  comment ";
    private static final String ENDING = " ; ";
    private static final String ALTER_TABLE = " alter table ";
    private static final String CREATE_TABLE = " create table ";
    private static final String AUTO_INCREMENT = " AUTO_INCREMENT ";
    private static final String EMPTY_STRING = "";


    /**
     * mysqlColumnMap {@link MysqlColumn}
     */


    public MysqlTable(List<SqlModel> models, String tableName, String tableDoc) {
        super(models, tableName, tableDoc);
    }

    public MysqlTable(List<SqlModel> models, String tableName) {
        super(models, tableName);
    }

    /**
     * 获取创建的sql语句
     *
     * @return
     */
    @Override
    public String buildCreateTableSql() {
        if (this.getModels().isEmpty()) {
            throw new RuntimeException("models isEmpty");
        }

        StringBuilder sql = new StringBuilder();
        //前缀
        sql.append(CREATE_TABLE).append(String.format(FORMAT, this.getTableName())).append(LINE_FEED_HAS_NEXT);

        SqlModel primaryKey = this.getPrimaryKey();
        //行数据拼接
        getModels().forEach(model -> sql.append(buildColumnDefinition(model)));
        //如果有主键拼接主键字符串，没有的话删除最后一个逗号
        if (primaryKey != null) {
            sql.append(String.format(PRIMARY_KEY_STR, getPrimaryKey().getColumn()));
        } else {
            sql.delete(sql.length() - LINE_FEED_HAS_NEXT.length(), sql.length() - 1);
        }
        //后缀
        sql.append(LINE_FEED_NOT_HAS_NEXT).append(buildEndingSql());
        return sql.toString();
    }


    /**
     * 构造行字段
     *
     * @param model {@link SqlModel}
     */
    private StringBuilder buildColumnDefinition(SqlModel model) {
        StringBuilder sql = new StringBuilder();
        sql.append(String.format(FORMAT, model.getColumn()));
        //获取当前模型的实际类型
        MysqlColumn mysqlColumn = model.getType();

        sql.append(mysqlColumn.isHasSuffix() ? String.format(mysqlColumn.getSuffix(), model.getLength()) : mysqlColumn.getSuffix());

        sql.append(model.isNull() ? ColumnStatus.ISNULL.getDescription() : ColumnStatus.NOTNULL.getDescription());
        //设置主键
        if (model.getPrimaryKey().isPrimaryKey()) {
            sql.append(AUTO_INCREMENT);
        }
        //设置时间默认值
        if (model.getType().getValue() == MysqlColumn.TIMESTAMP.getValue()) {
            sql.append(TIMESTAMP_DEFAULT);
        }
        return sql.append(LINE_FEED_HAS_NEXT);
    }

    /**
     * 拼接comment
     *
     * @return
     */
    public String builderCommentLine(SqlModel model) {
        if (StringUtils.isBlank(model.getComment())) {
            return "";
        }
        MysqlColumn mysqlColumn = MysqlColumn.enumMap.get(model.getType().getValue());
        //是否有后缀 (?) :有的话需要替换长度 部分字段不需要设置长度
        String suffix = mysqlColumn.isHasSuffix() ? String.format(mysqlColumn.getSuffix(), model.getLength()) : mysqlColumn.getSuffix();
        //设置TIMESTAMP的默认值 如果是这个类型需要追加
        suffix = mysqlColumn == MysqlColumn.TIMESTAMP ? suffix + TIMESTAMP_DEFAULT : suffix;
        return ALTER_TABLE + this.getTableName() + MODIFY + String.format(FORMAT, model.getColumn()) + " " + suffix + " " + COMMENT + " '" + model.getComment() + "'" + ENDING + "";
    }


    /**
     * 更新column字段
     *
     * @param model
     * @return
     */
    @Override
    public String getUpdateColumnDocSql(SqlModel model) {
        return builderCommentLine(model);
    }


    @Override
    public String updateOrInsertTableCommentSql(String tableName, String tableDesc) {
        return ALTER_TABLE + tableName + " " + COMMENT + " '" + tableDesc + "'" + ENDING + "";
    }

    /**
     * 构造结束语句 ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
     */
    public StringBuilder buildEndingSql() {
        StringBuilder sql = new StringBuilder();
        sql.append(") ENGINE=").append(this.getEngine()).append(this.getCharacter());
        return sql;
    }

    /**
     * 行备注的构造
     *
     * @return
     */
    @Override
    public String getCommentColumnSql() {
        StringBuilder sql = new StringBuilder();
        for (SqlModel model : this.getModels()) {
            sql.append(builderCommentLine(model)).append("\n");
        }
        return sql.toString();
    }


    /**
     * 表备注的构造
     *
     * @return
     */
    @Override
    public String getCommentTableSql() {
        return updateOrInsertTableCommentSql(this.getTableName(), this.getTableDesc());
    }

}
