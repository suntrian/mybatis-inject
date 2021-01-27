package com.quantchi.sqlinject;

import com.quantchi.sqlinject.annotation.Logic;
import com.quantchi.sqlinject.parser._Dialect;
import com.quantchi.sqlinject.parser.common.SqlRewriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHiveRewriter {

    @Test
    public void testRewriteSelect() {
        SqlRewriter inject = _Dialect.HIVE.getSqlRewriter();

        String sql1 = "SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y WHERE x  = ${abcd} AND y = ${abcd} ";
        sql1 = inject.injectFilter(sql1, "T1", "t1.a = 5" );
        System.out.println(sql1);
        Assert.assertEquals("SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n WHERE t1.a = 5 ) t2 on t1.b = t2.y WHERE t1.a = 5 AND x  = ${abcd} AND y = ${abcd} ".replaceAll(" ", "").toUpperCase(),
                sql1.replaceAll(" ", "").toUpperCase());

        String sql2 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y";
        sql2 = inject.injectFilter(sql2, null, "t1.a = 5" );
        System.out.println(sql2);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y WHERE t1.a=5".replaceAll(" ","").toUpperCase(),
                sql2.replaceAll(" ", "").toUpperCase());

        String sql3 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 T2 JOIN  table3 t1 ON t2.m = t1.n  ) T2 on t1.b = t2.y WHERE x  = 1234";
        sql3 = inject.injectFilter(sql3, "T2", "t2.a = 5" );
        System.out.println(sql3);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 T2 JOIN  table3 t1 ON t2.m = t1.n WHERE t2.a = 5  ) T2 on t1.b = t2.y WHERE t2.a = 5 AND  x  = 1234".replaceAll(" ", "").toUpperCase(),
                sql3.replaceAll(" ", "").toUpperCase());

        String sql4 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y WHERE x  = 1234";
        sql4= inject.injectFilter(sql4, "t3", "a = 5" );
        System.out.println(sql4);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n WHERE a = 5 ) t2 on t1.b = t2.y WHERE x  = 1234".replaceAll(" ", "").toUpperCase(),
                sql4.replaceAll(" ", "").toUpperCase());

        String sql5 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y WHERE x  = ${abcd}";
        sql5= inject.injectFilter(sql5, "table3", "a = 5" );
        System.out.println(sql5);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n WHERE a = 5 ) t2 on t1.b = t2.y WHERE x  = ${abcd}".replaceAll(" ", "").toUpperCase(),
                sql5.replaceAll(" ", "").toUpperCase());

        String sql6 = "SELECT t1.a, t1.b , t3.c FROM table1 t1 " +
                "           JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y " +
                "           JOIN table1 t3 ON t1.a = t3.b " +
                "           JOIN table1 t4 ON t3.a = t4.b " +
                " WHERE x  = ${abcd}";
        sql6= inject.injectFilter(sql6, "table1", "/**PREFIX**/a = 5" );
        System.out.println(sql6);
        Assert.assertEquals(("SELECT t1.a, t1.b , t3.c FROM table1 t1 " +
                        "           JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y " +
                        "           JOIN table1 t3 ON t1.a = t3.b " +
                        "           JOIN table1 t4 ON t3.a = t4.b " +
                        " WHERE (t1.a = 5 AND t3.a = 5 AND t4.a = 5) AND  x  = ${abcd}").replaceAll("[ \t\r\n]+", "").toUpperCase(),
                sql6.replaceAll(" ", "").toUpperCase());

    }

    @Test
    public void tesDualFiltertInject() {
        SqlRewriter inject = _Dialect.HIVE.getSqlRewriter();

        String sql1 = "SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y WHERE x  = ${abcd} AND y = ${abcd} ";
        Map<String, List<String>> tableFilters = new HashMap<>();
        tableFilters.put("t1", Arrays.asList("a = 1", "b = 1"));
        tableFilters.put(null, Arrays.asList("t1.a = 5", "t2.y = 5"));
        sql1 = inject.injectFilters(sql1, Logic.OR, tableFilters);
        System.out.println(sql1);
        Assert.assertEquals("SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n WHERE (a = 1 OR b = 1)  ) t2 on t1.b = t2.y WHERE ( a = 1 OR b = 1 OR t1.a = 5 OR t2.y = 5 ) AND  x  = ${abcd} AND y = ${abcd}"
                        .replaceAll(" ", "").toUpperCase(),
                sql1.replaceAll(" ", "").toUpperCase());


        String sql2 = "SELECT a,b ,c FROM table1 T1 " +
                "JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y " +
                "JOIN table1 T3 ON t2.y = t3.c  " +
                "WHERE x  = ${abcd} AND y = ${abcd} ";
        Map<String, List<String>> tableFilters2 = new HashMap<>();
        tableFilters2.put("table1", Arrays.asList("/**PREFIX**/a = 1", "/**PREFIX**/b = 1"));
        tableFilters2.put(null, Arrays.asList("t1.a = 5", "t2.y = 5"));
        sql2 = inject.injectFilters(sql2, Logic.OR, tableFilters2);
        System.out.println(sql2);
        Assert.assertEquals("SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y JOIN table1 T3 ON t2.y = t3.c  WHERE  ( (T1.a = 1 AND T3.a = 1) OR (T1.b = 1 AND T3.b = 1) OR t1.a = 5 OR t2.y = 5 )  AND  x  = ${abcd} AND y = ${abcd}"
                        .replaceAll(" ", "").toUpperCase(),
                sql2.replaceAll(" ", "").toUpperCase());

    }

}
