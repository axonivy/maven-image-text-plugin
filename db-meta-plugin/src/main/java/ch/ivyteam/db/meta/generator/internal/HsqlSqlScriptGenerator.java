package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlForeignKeyAction;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlTrigger;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.util.collections.CollectionsUtil;

/**
 * Generates the sql script for HSQL database systems
 * @author rwei
 */
public class HsqlSqlScriptGenerator extends SqlScriptGenerator
{
  /** 
   * Database System Hint addition triggers for tables: 
   * Specifies other tables for wich also trigger with the same java trigger class
   * should be generated
   */
  public static final String ADDITIONAL_TRIGGERS_FOR_TABLES = String.valueOf("AdditionalTriggersForTables");
  /**
   *  Database System Hint trigger name post fix:
   *  Adds the given post fix to the name of the trigger
   */
  public static final String TRIGGER_NAME_POST_FIX = String.valueOf("TriggerNamePostFix");
  /** Database System */ 
  public static final String HSQL_DB = String.valueOf("HsqlDb");
  /** 
   * Database System Hint Trigger Class: 
   * Specifies the java trigger class used in hsql triggers
   */
  public static final String TRIGGER_CLASS = String.valueOf("TriggerClass");

  /**
   * @see SqlScriptGenerator#generateDataType(PrintWriter, DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case CLOB:
        pr.append("LONGVARCHAR");
        break;
      case BLOB:
        pr.append("VARBINARY");
        break;
     default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateDataType(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlDataType, ch.ivyteam.db.meta.model.internal.SqlArtifact)
   */
  @Override
  protected void generateDataType(PrintWriter pr, SqlDataType dataType, SqlArtifact artifact)
  {
    if (artifact.getDatabaseManagementSystemHints(HSQL_DB).isHintSet(DATA_TYPE))
    {
      pr.print(artifact.getDatabaseManagementSystemHints(HSQL_DB).getHintValue(DATA_TYPE));
    }
    else
    {
      super.generateDataType(pr, dataType, artifact);
    }
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "HsqlDb";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isIndexInTableSupported()
   */
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isNullBeforeDefaultConstraint()
   */
  @Override
  protected boolean isNullBeforeDefaultConstraint()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isForeignKeyReferenceInColumnDefinitionSupported()
   */
  @Override
  public boolean isForeignKeyReferenceInColumnDefinitionSupported()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddForeignKey(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlForeignKey)
   */
  @Override
  public void generateAlterTableAddForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey) throws MetaException
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" ADD FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateSqlCaseExpression(PrintWriter, SqlCaseExpr)
   */
  @Override
  protected void generateSqlCaseExpression(PrintWriter pr, SqlCaseExpr caseExpr)
  {
    pr.print("CASEWHEN(");
    pr.print(caseExpr.getColumnName());
    pr.print(", ");
    pr.print(caseExpr.getWhenThenList().get(0).getColumnName());
    pr.print(", ");
    pr.print(caseExpr.getWhenThenList().get(1).getColumnName());
    pr.print(")");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachStatementDeleteTriggers(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  protected void generateForEachStatementDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition)
  {
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlTrigger trigger : table.getTriggers())
      {
        pr.print("CREATE TRIGGER ");
        pr.print(trigger.getTableName());
        pr.println("DeleteTrigger");
        pr.print("AFTER DELETE ON ");
        pr.print(trigger.getTableName());
        pr.println(" QUEUE 0");
        pr.print("CALL \"");
        pr.print(trigger.getDatabaseManagementSystemHints(HSQL_DB).getHintValue(TRIGGER_CLASS));
        pr.print("\"");
        generateDelimiter(pr);
        pr.println();
        pr.println();
      }
    }
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachRowDeleteTriggers(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  protected void generateForEachRowDeleteTriggers(PrintWriter pr, SqlMeta metaDefinition) throws MetaException
  {
    List<String> tables = new ArrayList<String>();
    for (SqlTable table : metaDefinition.getArtifacts(SqlTable.class))
    {
      for (SqlForeignKey foreignKey : table.getForeignKeys())
      {
        if ((!isDatabaseSystemHintSet(foreignKey, NO_REFERENCE))&&(getForeignKeyAction(foreignKey) == SqlForeignKeyAction.ON_DELETE_THIS_CASCADE))
        {
          tables.clear();
          tables.add(table.getId());
          if (isDatabaseSystemHintSet(foreignKey, ADDITIONAL_TRIGGERS_FOR_TABLES))
          {
            for (String tableName : getDatabaseSystemHintValue(foreignKey, ADDITIONAL_TRIGGERS_FOR_TABLES).split(","))
            {
              tables.add(tableName.trim());
            }
          }
          for (String tableName : tables)
          {
            pr.print("CREATE TRIGGER ");
            pr.print(tableName);
            if (isDatabaseSystemHintSet(foreignKey, TRIGGER_NAME_POST_FIX))
            {
              generateDatabaseManagementHintValue(pr, foreignKey, TRIGGER_NAME_POST_FIX);
            }
            pr.println("DeleteTrigger");
            pr.print("AFTER DELETE ON ");
            pr.print(tableName);
            pr.println(" QUEUE 0");
            pr.print("CALL \"");
            generateDatabaseManagementHintValue(pr, foreignKey, TRIGGER_CLASS);
            pr.print("\"");
            generateDelimiter(pr);
            pr.println();
            pr.println();
          }
        }
      }
    }
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return CollectionsUtil.listify(HSQL_DB);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIdentifier(java.io.PrintWriter, java.lang.String)
   */
  @Override
  protected void generateIdentifier(PrintWriter pr, String identifier)
  {
    if (isReservedSqlKeyword(identifier))
    {
      identifier = identifier.toUpperCase();
    }
    super.generateIdentifier(pr, identifier);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return ":old";
  }
  
  /**
   * Could overridden from different database types
   * @param pr
   * @param newColumn 
   * @param newTable
   * @param oldColumn 
   * @throws MetaException 
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn) throws MetaException
  {
    GenerateAlterTableUtil.generateAlterTableChangeColumnWithDefaultAndNullConstraints(pr, this, newColumn, newTable, "ALTER COLUMN");
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("CALL \"ch.ivyteam.ivy.persistence.db.hsqldb.HsqlStoredProcedure.dropUniqueConstraints\"('"+table.getId()+"')");
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("CALL \"ch.ivyteam.ivy.persistence.db.hsqldb.HsqlStoredProcedure.dropForeignKey\"('"+table.getId()+"', '"+foreignKey.getColumnName()+"')");
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }
  
  @Override
  public boolean isRecreationOfForeignKeysOnAlterTableNeeded()
  {
    return true;
  }
 
}