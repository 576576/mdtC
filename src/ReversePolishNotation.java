import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class ReversePolishNotation {
    static void main() {
        String str = "u.cer==@unit";
        IO.println(Arrays.toString(generateRpn(str)));
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

