# inference-engine
Inference Engine using first order resolution in Java.

Input-Format:
Number of Queries(n)
Query 1
...
Query n
Number of Sentences in KB(k)
Sentence 1
...
Sentence k

Output-Format:
Answer 1
...
Answer n
where each answer is either TRUE or FALSE.

Description:
Each query should be a single literal of the form:
Predicate(Constant) or ~Predicate(Constant)
where predicates and constant start with an uppercase letter and variables are single lowercase letters.

Sample-Input:
2
Ancestor(Liz,Billy)
Ancestor(Liz,Joe)
6
Mother(Liz,Charley)
Father(Charley,Billy)
~Mother(x,y) | Parent(x,y)
~Father(x,y) | Parent(x,y)
~Parent(x,y) | Ancestor(x,y)
~Parent(x,y) | ~Ancestor(y,z) | Ancestor(x,z)

Sample-Output:
TRUE
FALSE
