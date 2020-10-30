package com.quantchi.sqlinject;

import com.quantchi.sqlinject.mysql.MysqlInject;
import org.junit.Assert;
import org.junit.Test;

public class TestMysqlRewriter {

    @Test
    public void testRewriteSelect() {
        MysqlInject inject = new MysqlInject();

        String sql1 = "SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y WHERE x  = ? AND y = ? ";
        sql1 = inject.injectFilter(sql1, "T1", "t1.a = 5" );
        System.out.println(sql1);
        Assert.assertEquals("SELECT a,b ,c FROM table1 T1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n WHERE t1.a = 5 ) t2 on t1.b = t2.y WHERE (t1.a = 5) AND x  = ? AND y = ? ".replaceAll(" ", "").toUpperCase(),
                sql1.replaceAll(" ", "").toUpperCase());

        String sql2 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y";
        sql2 = inject.injectFilter(sql2, null, "t1.a = 5" );
        System.out.println(sql2);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t1 ON t2.m = t1.n  ) t2 on t1.b = t2.y WHERE t1.a=5".replaceAll(" ","").toUpperCase(),
                sql2.replaceAll(" ", "").toUpperCase());

        String sql3 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 T2 JOIN  table3 t1 ON t2.m = t1.n  ) T2 on t1.b = t2.y WHERE x  = 1234";
        sql3 = inject.injectFilter(sql3, "T2", "t2.a = 5" );
        System.out.println(sql3);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 T2 JOIN  table3 t1 ON t2.m = t1.n WHERE t2.a = 5  ) T2 on t1.b = t2.y WHERE (t2.a = 5) AND  x  = 1234".replaceAll(" ", "").toUpperCase(),
                sql3.replaceAll(" ", "").toUpperCase());

        String sql4 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y WHERE x  = 1234";
        sql4= inject.injectFilter(sql4, "t3", "a = 5" );
        System.out.println(sql4);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n WHERE a = 5 ) t2 on t1.b = t2.y WHERE x  = 1234".replaceAll(" ", "").toUpperCase(),
                sql4.replaceAll(" ", "").toUpperCase());

        String sql5 = "SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y WHERE x  = ?";
        sql5= inject.injectFilter(sql5, "table3", "a = 5" );
        System.out.println(sql5);
        Assert.assertEquals("SELECT a,b ,c FROM table1 t1 JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n WHERE a = 5 ) t2 on t1.b = t2.y WHERE x  = ?".replaceAll(" ", "").toUpperCase(),
                sql5.replaceAll(" ", "").toUpperCase());

        String sql6 = "SELECT t1.a, t1.b , t3.c FROM table1 t1 " +
                "           JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y " +
                "           JOIN table1 t3 ON t1.a = t3.b " +
                "           JOIN table1 t4 ON t3.a = t4.b " +
                " WHERE x  = ?";
        sql6= inject.injectFilter(sql6, "table1", "/**PREFIX**/a = 5" );
        System.out.println(sql6);
        Assert.assertEquals(("SELECT t1.a, t1.b , t3.c FROM table1 t1 " +
                        "           JOIN (select x, y, z from table2 t2 JOIN  table3 t3 ON t2.m = t3.n  ) t2 on t1.b = t2.y " +
                        "           JOIN table1 t3 ON t1.a = t3.b " +
                        "           JOIN table1 t4 ON t3.a = t4.b " +
                        " WHERE (t1.a = 5 AND t3.a = 5 AND t4.a = 5) AND  x  = ?").replaceAll("[ \t\r\n]+", "").toUpperCase(),
                sql6.replaceAll(" ", "").toUpperCase());

    }

    @Test
    public void testInject() {
        String sql = "SELECT  * " +
                "        FROM STD_STANDARD S\n" +
                "         \n" +
                "         WHERE  S.ENTITY_STATUS NOT IN ('0','3','4') \n" +
                "        ORDER BY ID DESC\n" +
                " LIMIT ? ";


    }

}
