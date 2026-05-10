grammar Hll;

// ===== Parser Rules =====

program
    : declaration* EOF
    ;

declaration
    : typeDecl
    | structDecl
    | fnDecl
    | importDecl
    ;

// --- Type declarations ---
typeDecl
    : 'type' IDENT '=' typeExpr ('where' constraint)?
    ;

constraint
    : IDENT '(' expr ')'
    ;

// --- Struct declarations ---
structDecl
    : 'struct' IDENT '{' fieldDecl* '}'
    ;

fieldDecl
    : IDENT ':' typeExpr
    ;

// --- Function declarations ---
fnDecl
    : 'fn' IDENT '(' params? ')' ('->' typeExpr)? block
    ;

params
    : param (',' param)*
    ;

param
    : IDENT ':' typeExpr
    ;

// --- Import Java ---
importDecl
    : 'import' 'java' STRING 'as' IDENT ('{' javaMapping* '}')?
    ;

javaMapping
    : 'fn' IDENT '(' params? ')' '->' typeExpr
    ;

// --- Types ---
typeExpr
    : IDENT ('<' typeExpr '>')?
    ;

// --- Statements ---
block
    : '{' statement* '}'
    ;

statement
    : letStmt
    | returnStmt
    | matchStmt
    | ifStmt
    | exprStmt
    ;

letStmt
    : 'let' IDENT (':' typeExpr)? '=' expr
    ;

returnStmt
    : 'return' expr?
    ;

matchStmt
    : 'match' expr '{' matchArm+ '}'
    ;

matchArm
    : pattern '=>' (expr | block)
    ;

pattern
    : IDENT '(' IDENT ')'   // Some(x)
    | 'None'
    | IDENT
    | '_'
    ;

ifStmt
    : 'if' expr block ('else' block)?
    ;

exprStmt
    : expr
    ;

// --- Expressions ---
expr
    : primary                                   # primaryExpr
    | expr '.' IDENT                            # fieldAccess
    | expr '.' IDENT '(' args? ')'             # methodCall
    | expr '?'                                  # optionPropagate
    | expr op=('*' | '/') expr                  # mulDiv
    | expr op=('+' | '-') expr                  # addSub
    | expr op=('==' | '!=' | '<' | '>' | '<=' | '>=') expr  # comparison
    | expr op='&&' expr                         # logicAnd
    | expr op='||' expr                         # logicOr
    | '!' expr                                  # logicNot
    ;

primary
    : IDENT '(' args? ')'                       # fnCall
    | 'None'                                    # noneLit
    | IDENT                                     # identifier
    | STRING                                    # stringLit
    | NUMBER                                    # numberLit
    | FLOAT                                     # floatLit
    | ('true' | 'false')                        # boolLit
    | '(' expr ')'                              # parenExpr
    ;

args
    : expr (',' expr)*
    ;

// ===== Lexer Rules =====

// Keywords
TYPE    : 'type' ;
STRUCT  : 'struct' ;
FN      : 'fn' ;
LET     : 'let' ;
RETURN  : 'return' ;
MATCH   : 'match' ;
IF      : 'if' ;
ELSE    : 'else' ;
IMPORT  : 'import' ;
JAVA    : 'java' ;
AS      : 'as' ;
WHERE   : 'where' ;
NONE    : 'None' ;
TRUE    : 'true' ;
FALSE   : 'false' ;

// Identifiers and literals
IDENT   : [a-zA-Z_][a-zA-Z0-9_]* ;
STRING  : '"' (~["\\\r\n] | '\\' .)* '"' ;
FLOAT   : [0-9]+ '.' [0-9]+ ;
NUMBER  : [0-9]+ ;

// Symbols
ARROW   : '->' ;
FATARROW: '=>' ;
QMARK   : '?' ;
DOT     : '.' ;
COMMA   : ',' ;
COLON   : ':' ;
SEMI    : ';' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
LBRACE  : '{' ;
RBRACE  : '}' ;
LT      : '<' ;
GT      : '>' ;
EQ      : '==' ;
NEQ     : '!=' ;
ASSIGN  : '=' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
AND     : '&&' ;
OR      : '||' ;
NOT     : '!' ;
LE      : '<=' ;
GE      : '>=' ;

// Whitespace and comments
WS      : [ \t\r\n]+ -> skip ;
COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
