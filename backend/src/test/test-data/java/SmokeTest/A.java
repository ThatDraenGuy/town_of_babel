package test.test.a;


class A {
    /**
     * @param a
     * @param b
     * @param c
     * @return
     */
    int foo(int a, int b, int c) {
        /*
            Some commentlines

         */
        while (b < a) {
            if (c % b == 0) {
                for (int i = 0; i < c; i++) {
                    if (i + a == 1000) {
                        return 1;
                    }
                } // More comments
            } else {
                c++;
            }
            b *= a / c;
        }
    }


    int bar(int x) {
        return x + 2;
    }
}
