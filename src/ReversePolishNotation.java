import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class ReversePolishNotation {
    public static void main(String[] args) {
        //测试用例
        //String str = "1+2";//12+
        //String str = "1+2*3-4*5-6+7*8-9"; //123*+45*-6-78*+9-
        //String str = "a*(b-c*d)+e-f/g*(h+i*j-k)"; // abcd*-*e+fg/hij*+k-*-
        //String str = "6*(5+(2+3)*8+3)"; //6523+8*+3+*
        //String str = "a+b*c+(d*e+f)*g"; //abc*+de*f+g*f
        //String str = "a2*(b-c*d)>=e-f//g*(h+i*j.^k)";
        String str = "re>=1+isd2//5";

        System.out.println(Arrays.toString(generateRpn(str)));
    }

    public static String[] generateRpn(String str) {
        return rpn(new Stack<>(), Utils.stringSplit(str));
    }


    /**
     * 将中置表达式转为逆波兰表达式
     *
     * @return 逆波兰表达式
     */
    public static String[] rpn(Stack<String> operators, String[] tokens) {
        ArrayList<String> output = new ArrayList<>();
        for (String token : tokens) {
            if (Utils.Operator.isOperator(token)) {
                if (token.equals("(")) {
                    operators.push(token);
                } else if (token.equals(")")) {
                    while (!operators.empty() && !operators.peek().equals("(")) {
                        output.add(operators.pop());
                    }
                    if (!operators.empty() && operators.peek().equals("(")) {
                        operators.pop();
                    }
                } else {
                    while (!operators.empty() && !operators.peek().equals("(") && Utils.Operator.cmp(token, operators.peek()) <= 0) {
                        output.add(operators.pop());
                    }
                    operators.push(token);
                }
            } else {
                output.add(token);
            }
        }

        // 遍历结束，将运算符栈全部压入output
        while (!operators.empty()) {
            if (!operators.peek().equals("(")) {
                output.add(operators.pop());
            } else {
                operators.pop(); // Remove any remaining left parentheses
            }
        }
        return output.toArray(new String[0]);
    }

}

