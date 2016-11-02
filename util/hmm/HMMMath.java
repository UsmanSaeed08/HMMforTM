package util.hmm;

/**
 * Class provide functions for operations over probablities that presented in
 * logarithmic form. Such way of calculations gives precession stability and
 * quite fast as compared with simple algebraic operations.
 */
public class HMMMath {
    public static final double MIN_NOT_NULL_PROBAB = 1e-16;
    public static final double LOG_NEG_INF = -1e7;
    //
    private static final double LOG_1_EXP_MIN_NOT_NULL_ARG = -37;
    private static final double LOG_1_EXP_APROX_STEP = 1e-2;
    private static final double[] log_1_exp_func;
    private static final double[] log_1_exp_deriv;

    static {
        int point_count = (int)((0-LOG_1_EXP_MIN_NOT_NULL_ARG)/LOG_1_EXP_APROX_STEP)+1;
        log_1_exp_func = new double[point_count];
        log_1_exp_deriv = new double[point_count];
        for(int i=0;i<point_count;i++) {
            double x = -i*LOG_1_EXP_APROX_STEP;
            log_1_exp_func[i] = Math.log(1+Math.exp(x));
            log_1_exp_deriv[i] = Math.exp(x)/(1+Math.exp(x));
        }
    }

    private HMMMath() {}

    public static void main(String[] args) {
        // Testing
        double max_err = 0;
        double step = LOG_1_EXP_APROX_STEP/10;
        for(double x=0;x>=LOG_1_EXP_MIN_NOT_NULL_ARG;x-=step) {
            double apr = getAproxForLogOneExp(x);
            double f = Math.log(1+Math.exp(x));
            double err = Math.abs(f-apr);
            if(max_err<err) max_err = err;
        }
        System.out.println("max_err="+max_err);
    }

    public static double log(double probab) {
        return (probab<MIN_NOT_NULL_PROBAB)?LOG_NEG_INF:Math.log(probab);
    }

    /**
     * Fast (and approximate) calculation of log(exp(log_p)+exp(log_q)). The idea is not to do exps and logs but just
     * return log(exp(a)*(1+exp(x))) = a + log(1+exp(x)), where a = max{log_p,log_q}, x = min{log_p,log_q} - a.
     * Value of log(1+exp(x)) is caleculated in getAproxForLogOneExp() using precomputed tables and cubic interpolation.
     * @param log_p first probability
     * @param log_q second probability
     * @return returns ~ log(exp(log_p)+exp(log_q))
     */
    public static double logSum(double log_p,double log_q) {
        double ret;
        double x;
        if(log_p>=log_q) {
            ret = log_p;
            x = log_q-log_p;
        }
        else {
            ret = log_q;
            x = log_p-log_q;
        }
        if(x<LOG_1_EXP_MIN_NOT_NULL_ARG) return ret;
        double aprox_log_1_exp = getAproxForLogOneExp(x);  //Math.log(1+Math.exp(x));
        return ret+aprox_log_1_exp;
    }

    /**
     * Cubic interpolation of function for argument x between points x1 and x2.
     * @param x1 left point
     * @param x2 right point
     * @param f1 function for x1
     * @param f2 function for x2
     * @param d1 derivative of function for x1
     * @param d2 derivative of function for x2
     * @param x argument where we need to know function value
     * @return function for x
     */
    public static double getCubicAprox(double x1,double x2,double f1,double f2,
                                       double d1,double d2,double x) {
        double dif = x2-x1;
        double p1 = (x2-x)*(x2-x)*(x2-3*x1+2*x);
        double p2 = (x-x1)*(x-x1)*(3*x2-x1-2*x);
        double q1 = (x-x1)*(x-x2)*(x-x2);
        double q2 = (x-x2)*(x-x1)*(x-x1);
        return (f1*p1+f2*p2)/(dif*dif*dif)+(d1*q1+d2*q2)/(dif*dif);
    }

    /**
     * Function compute value of log(1+exp(x)) using precomputed tables and cubic interpolation.
     * @param x argument
     * @return fast aproximation for log(1+exp(x))
     */
    public static double getAproxForLogOneExp(double x) {
        if(x>0) throw new RuntimeException("Error calculating log_1_exp equation");
        int node2 = (int)(-x/LOG_1_EXP_APROX_STEP);
        if(node2+1>=log_1_exp_func.length) return 0;
        double x2 = -node2*LOG_1_EXP_APROX_STEP;
        double x1 = x2-LOG_1_EXP_APROX_STEP;
        double f1 = log_1_exp_func[node2+1];
        double f2 = log_1_exp_func[node2];
        double d1 = log_1_exp_deriv[node2+1];
        double d2 = log_1_exp_deriv[node2];
        return getCubicAprox(x1,x2,f1,f2,d1,d2,x);
    }
}
