package org.flymine.sql.query;

import java.util.*;
import java.io.*;
import antlr.collections.AST;

/**
 * Represents an SQL query in parsed form.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class Query implements SQLStringable
{
    protected List select;
    protected Set from;
    protected Set where;
    protected Set groupBy;
    protected Set having;
    protected List orderBy;
    protected int limit;
    protected int offset;
    protected boolean explain;
    protected boolean distinct;

    private Map aliasToTable;

    /**
     * Construct a new Query.
     */
    public Query() {
        select = new ArrayList();
        from = new HashSet();
        where = new HashSet();
        groupBy = new HashSet();
        having = new HashSet();
        orderBy = new ArrayList();
        limit = 0;
        offset = 0;
        explain = false;
        distinct = false;
        aliasToTable = new HashMap();
    }

    /**
     * Construct a new parsed Query from a String.
     *
     * @param sql a SQL SELECT String to parse
     * @throws antlr.RecognitionException if the text is not recognised properly
     * @throws antlr.TokenStreamException if something else goes wrong
     */
    public Query(String sql) throws antlr.RecognitionException, antlr.TokenStreamException {
        select = new ArrayList();
        from = new HashSet();
        where = new HashSet();
        groupBy = new HashSet();
        having = new HashSet();
        orderBy = new ArrayList();
        limit = 0;
        offset = 0;
        explain = false;
        distinct = false;
        aliasToTable = new HashMap();

        InputStream is = new ByteArrayInputStream(sql.getBytes());
        
        SqlLexer lexer = new SqlLexer(is);
        SqlParser parser = new SqlParser(lexer);
        parser.start_rule();

        AST ast = parser.getAST();
        if (ast.getType() != SqlTokenTypes.SQL_STATEMENT) {
            throw (new IllegalArgumentException("Expected: a SQL SELECT statement"));
        }
        processAST(ast.getFirstChild());
    }

    /**
     * Gets the current distinct status of this query.
     *
     * @return true if this query is distinct
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Sets the distinct status of this query.
     * 
     * @param distinct the new distinct status
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }
    
    /**
     * Gets the current explain status of this query.
     *
     * @return true if this query is an explain
     */
    public boolean isExplain() {
        return explain;
    }

    /**
     * Sets the explain status of this query.
     *
     * @param explain the new explain status
     */
    public void setExplain(boolean explain) {
        this.explain = explain;
    }

    /**
     * Gets the list of select fields for this query.
     *
     * @return a List of SelectValue objects representing the select list of the query
     */
    public List getSelect() {
        return select;
    }

    /**
     * Adds a field to the select list of this query. Fields are stored in a List in the order they
     * are added.
     *
     * @param obj a SelectValue to add to the list
     */
    public void addSelect(SelectValue obj) {
        select.add(obj);
    }

    /**
     * Gets the Set of from tables for this query.
     *
     * @return a Set of AbstractTable objects representing the from list of the query
     */
    public Set getFrom() {
        return from;
    }

    /**
     * Adds a table to the from list of this query. The order is not important.
     *
     * @param obj an AbstractTable to add to the set
     */
    public void addFrom(AbstractTable obj) {
        from.add(obj);
        aliasToTable.put(obj.getAlias(), obj);
    }

    /**
     * Gets the Set of constraints in the where clause of this query.
     *
     * @return a Set of AbstractConstraint objects which, ANDed together form the where clause
     */
    public Set getWhere() {
        return where;
    }

    /**
     * Adds a constraint to the where clause for this query. The order is not important. The
     * constraints in the Set formed are ANDed together to form the where clause. If you wish to OR
     * constraints together, use a ConstraintSet.
     *
     * @param obj an AbstractConstraint to add to the where clause
     */
    public void addWhere(AbstractConstraint obj) {
        where.add(obj);
    }

    /**
     * Gets the Set of fields in the GROUP BY clause of this query.
     *
     * @return a Set of AbstractValue objects representing the GROUP BY clause
     */
    public Set getGroupBy() {
        return groupBy;
    }

    /**
     * Adds a field to the GROUP BY clause of this query. The order is not important.
     *
     * @param obj an AbstractValue to add to the GROUP BY clause
     */
    public void addGroupBy(AbstractValue obj) {
        groupBy.add(obj);
    }

    /**
     * Gets the set of constraints forming the HAVING clause of this query.
     *
     * @return a Set of AbstractConstraints representing the HAVING clause
     */
    public Set getHaving() {
        return having;
    }

    /**
     * Adds a constraint to the HAVING clause of this query. The order is not important.
     *
     * @param obj an AbstractConstraint to add to the HAVING clause
     */
    public void addHaving(AbstractConstraint obj) {
        having.add(obj);
    }
    
    /**
     * Gets the list of fields forming the ORDER BY clause of this query.
     *
     * @return a List of AbstractValues representing the ORDER BY clause
     */
    public List getOrderBy() {
        return orderBy;
    }

    /**
     * Adds a field to the ORDER BY clause of this query. The fields are repesented in the clause in
     * the order they were added.
     *
     * @param obj an AbstractValue to add to the ORDER BY clause
     */
    public void addOrderBy(AbstractValue obj) {
        orderBy.add(obj);
    }

    /**
     * Gets the LIMIT number for this query.
     *
     * @return the maximum number of rows that this query is allowed to return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Gets the OFFSET number for this query.
     *
     * @return the number of rows in the query to discard before returning the first result
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the LIMIT and OFFSET numbers for this query.
     *
     * @param limit the LIMIT number
     * @param offset the OFFSET number
     */
    public void setLimitOffset(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }
    
    /**
     * Convert this Query into a SQL String query.
     *
     * @return this Query in String form
     */
    public String getSQLString() {
        return (explain ? "EXPLAIN " : "") + "SELECT " + (distinct ? "DISTINCT " : "")
            + collectionToSQLString(select, ", ")
            + (from.isEmpty() ? "" : " FROM " + collectionToSQLString(from, ", "))
            + (where.isEmpty() ? "" : " WHERE " + collectionToSQLString(where, " AND "))
            + (groupBy.isEmpty() ? "" : " GROUP BY " + collectionToSQLString(groupBy, ", ")
                + (having.isEmpty() ? "" : " HAVING " + collectionToSQLString(having, " AND ")))
            + (orderBy.isEmpty() ? "" : " ORDER BY " + collectionToSQLString(orderBy, ", "))
            + (limit == 0 ? "" : " LIMIT " + limit
                + (offset == 0 ? "" : " OFFSET " + offset));
    }

    /**
     * Converts a collection of objects that implement the getSQLString method into a String,
     * with the given comma string between each element.
     *
     * @param c the Collection on objects
     * @param comma the String to use as a separator between elements
     * @return a String representation
     */
    protected static String collectionToSQLString(Collection c, String comma) {
        String retval = "";
        boolean needComma = false;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            SQLStringable o = (SQLStringable) iter.next();
            if (needComma) {
                retval += comma;
            }
            needComma = true;
            retval += o.getSQLString();
        }
        return retval;
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is equivalent
     */
    public boolean equals(Object obj) {
        if (obj instanceof Query) {
            Query q = (Query) obj;
            return select.equals(q.select) && from.equals(q.from) && where.equals(q.where)
                && groupBy.equals(q.groupBy) && having.equals(q.having) && orderBy.equals(q.orderBy)
                && (limit == q.limit) && (offset == q.limit) && (explain == q.explain)
                && (distinct == q.distinct);
        }
        return false;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer created from the contents of the Query
     */
    public int hashCode() {
        return (3 * select.hashCode()) + (5 * from.hashCode())
            + (7 * where.hashCode()) + (11 * groupBy.hashCode())
            + (13 * having.hashCode()) + (17 * orderBy.hashCode()) + (19 * limit) + (23 * offset)
            + (explain ? 29 : 0) + (distinct ? 31 : 0);
    }

    /**
     * Processes an AST node produced by antlr, at the top level of the SQL query.
     *
     * @param ast an AST node to process
     */
    private void processAST(AST ast) {
        boolean processSelect = false;
        switch (ast.getType()) {
            case SqlTokenTypes.LITERAL_select:
            case SqlTokenTypes.LITERAL_from:
            case SqlTokenTypes.SEMI:
            case SqlTokenTypes.OPEN_PAREN:
            case SqlTokenTypes.CLOSE_PAREN:
                break;
            case SqlTokenTypes.SELECT_LIST:
                processSelect = true;
                break;
            case SqlTokenTypes.FROM_LIST:
                processFromList(ast.getFirstChild());
                break;
            case SqlTokenTypes.WHERE_CLAUSE:
//                processWhereCondition(astgetFirstChild());
                break;
            default:
                throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]"));
        }
        if (ast.getNextSibling() != null) {
            processAST(ast.getNextSibling());
        }
        if (processSelect) {
            processSelectList(ast.getFirstChild());
        }
    }

    /**
     * Processes an AST node that describes a FROM list.
     *
     * @param ast an AST node to process
     */
    private void processFromList(AST ast) {
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.LITERAL_from:
                case SqlTokenTypes.COMMA:
                    break;
                case SqlTokenTypes.TABLE:
                    processNewTable(ast.getFirstChild());
                    break;
                case SqlTokenTypes.SUBQUERY:
                    processNewSubQuery(ast.getFirstChild());
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a table in the FROM list.
     *
     * @param ast an AST node to process
     */
    private void processNewTable(AST ast) {
        String tableName = null;
        String tableAlias = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.LITERAL_as:
                    break;
                case SqlTokenTypes.TABLE_NAME:
                    tableName = ast.getFirstChild().getText();
                    break;
                case SqlTokenTypes.TABLE_ALIAS:
                    tableAlias = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        addFrom(new Table(tableName, tableAlias));
    }
    
    /**
     * Processes an AST node that describes a subquery in the FROM list.
     *
     * @param ast an AST node to process
     */
    private void processNewSubQuery(AST ast) {
        AST subquery = null;
        String alias = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.LITERAL_as:
                case SqlTokenTypes.OPEN_PAREN:
                case SqlTokenTypes.CLOSE_PAREN:
                    break;
                case SqlTokenTypes.SQL_STATEMENT:
                    subquery = ast.getFirstChild();
                    break;
                case SqlTokenTypes.TABLE_ALIAS:
                    alias = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        Query q = new Query();
        q.processAST(subquery);
        addFrom(new SubQuery(q, alias));
    }
    
    /**
     * Processes an AST node that describes a SELECT list.
     *
     * @param ast an AST node to process
     */
    public void processSelectList(AST ast) {
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.COMMA:
                    break;
                case SqlTokenTypes.SELECT_VALUE:
                    processNewSelect(ast.getFirstChild());
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a SelectValue.
     *
     * @param ast an AST node to process
     */
    public void processNewSelect(AST ast) {
        AbstractValue v = null;
        String alias = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.LITERAL_as:
                    break;
                case SqlTokenTypes.FIELD_ALIAS:
                    alias = ast.getFirstChild().getText();
                    break;
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    v = processNewAbstractValue(ast);
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        SelectValue sv = new SelectValue(v, alias);
        addSelect(sv);
    }

    /**
     * Processes a single AST node that describes an AbstractValue.
     *
     * @param ast as AST node to process
     * @return an AbstractValue object corresponding to the input
     */
    public AbstractValue processNewAbstractValue(AST ast) {
        switch (ast.getType()) {
            case SqlTokenTypes.FIELD:
                return processNewField(ast.getFirstChild());
            case SqlTokenTypes.CONSTANT:
                return new Constant(ast.getFirstChild().getText());
            case SqlTokenTypes.UNSAFE_FUNCTION:
                return processNewUnsafeFunction(ast.getFirstChild());
            case SqlTokenTypes.SAFE_FUNCTION:
                return processNewSafeFunction(ast.getFirstChild());
            default:
                throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]"));
        }
    }

    /**
     * Processes an AST node that describes a Field.
     *
     * @param ast an AST node to process
     * @return a Field object corresponding to the input
     */
    public Field processNewField(AST ast) {
        String table = null;
        String field = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.DOT:
                    break;
                case SqlTokenTypes.TABLE_ALIAS:
                    table = ast.getFirstChild().getText();
                    break;
                case SqlTokenTypes.FIELD_NAME:
                    field = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        AbstractTable t = (AbstractTable) aliasToTable.get(table);
        return new Field(field, t);
    }

    /**
     * Processes an AST node that describes an unsafe function.
     *
     * @param ast an AST node to process
     * @return a Function object corresponding to the input
     */
    public Function processNewUnsafeFunction(AST ast) {
        AbstractValue firstObj = null;
        Function retval = null;
        boolean gotType = false;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.OPEN_PAREN:
                case SqlTokenTypes.CLOSE_PAREN:
                    break;
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    if (!gotType) {
                        firstObj = processNewAbstractValue(ast);
                    } else {
                        retval.add(processNewAbstractValue(ast));
                    }
                    break;
                case SqlTokenTypes.PLUS:
                    if (!gotType) {
                        retval = new Function(Function.PLUS);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.MINUS:
                    if (!gotType) {
                        retval = new Function(Function.MINUS);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.ASTERISK:
                    if (!gotType) {
                        retval = new Function(Function.MULTIPLY);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.DIVIDE:
                    if (!gotType) {
                        retval = new Function(Function.DIVIDE);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.POWER:
                    if (!gotType) {
                        retval = new Function(Function.POWER);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.PERCENT:
                    if (!gotType) {
                        retval = new Function(Function.MODULO);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return retval;
    }

    /**
     * Processes an AST node that describes an unsafe function.
     *
     * @param ast an AST node to process
     * @return a Function object corresponding to the input
     */
    public Function processNewSafeFunction(AST ast) {
        Function retval = null;
        boolean gotType = false;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.OPEN_PAREN:
                case SqlTokenTypes.CLOSE_PAREN:
                case SqlTokenTypes.ASTERISK:
                    break;
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    retval.add(processNewAbstractValue(ast));
                    break;
                case SqlTokenTypes.LITERAL_count:
                    if (!gotType) {
                        retval = new Function(Function.COUNT);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_max:
                    if (!gotType) {
                        retval = new Function(Function.MAX);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_min:
                    if (!gotType) {
                        retval = new Function(Function.MIN);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_sum:
                    if (!gotType) {
                        retval = new Function(Function.SUM);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_avg:
                    if (!gotType) {
                        retval = new Function(Function.AVG);
                        gotType = true;
                    }
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return retval;
    }

    /**
     * A testing method - converts the argument into a Query object, and then converts it back to
     * a String again.
     *
     * @param args command-line arguments
     * @throws Exception anytime
     */
    public static void main(String args[]) throws Exception {
        PrintStream out = System.out;

        InputStream is = new ByteArrayInputStream(args[0].getBytes());
        SqlLexer lexer = new SqlLexer(is);
        SqlParser parser = new SqlParser(lexer);
        parser.start_rule();
        AST ast = parser.getAST();
        
        out.println("\n==> Dump of AST <==");
        antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();
        visitor.visit(ast);

        if (ast.getType() != SqlTokenTypes.SQL_STATEMENT) {
            throw (new IllegalArgumentException("Expected: a SQL SELECT statement"));
        }
        Query q = new Query();
        q.processAST(ast.getFirstChild());

        out.println("\n" + q.getSQLString());
    }
}
