package com.applane.resourceoriented.hris.utility;

public class ConvertNumberToWordsBusinessLogic {
	String[] unit1 = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine" };
	String[] unit2 = { "ten", "Eleven", "Twellve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Ninteen" };
	String[] unit3 = { "", "Ten", "Twenty", "Thirty", "Fourty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninty" };
	String[] unit4 = { "", "Hundred", "Thousand", "Lakhs", "Crore" };

	public String convertNumberToWords(long num) {
		int count = 1;
		String value = "";
		while (num != 0) {
			if (count < 4) {
				if (count % 2 == 0) {
					long a = num % 10;
					value = unit1[(int) a] + " " + value;
					num = num / 10;
					if (num > 0 && num % 100 != 0) {
						value = unit4[count] + " " + value;
					}
				} else {
					long a = num % 100;

					long b = 0;
					if (a > 10 && a < 20) {
						value = unit2[(int) (a % 10)] + " " + value;
					} else {
						b = a % 10;
						int c = (int) (a / 10);
						value = unit1[(int) b] + " " + value;
						value = unit3[c] + " " + value;
					}
					num = num / 100;
					if (num > 0 && num % 10 != 0) {
						value = unit4[count] + " " + value;
					}
				}
			} else {
				if (count % 2 != 0) {
					long a = num % 10;
					num = num / 10;
					value = unit1[(int) a] + " " + value;
					if (num > 0) {
						value = unit4[count] + " " + value;
					}
				} else {
					long a = num % 100;
					int b = 0;
					if (a > 10 && a < 20) {
						value = unit2[(int) (a % 10)] + " " + value;
					} else {
						b = (int) (a % 10);
						int c = (int) (a / 10);
						value = unit1[b] + " " + value;
						value = unit3[c] + " " + value;
					}
					num = num / 100;
					if (num > 0) {
						value = unit4[count] + " " + value;
					}

				}
			}
			count++;
		}
		return value;
	}

	public static void main(String[] args) {
		// System.out.println(new ConvertNumberToWordsBusinessLogic().convertNumberToWords(100301));
	}
}
