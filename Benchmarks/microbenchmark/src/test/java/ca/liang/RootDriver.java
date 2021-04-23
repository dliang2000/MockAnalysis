package ca.liang;

public class RootDriver {
	public static void main(String[] argv) {
		ca.liang.PayRollArrayMockTest class1 = new ca.liang.PayRollArrayMockTest();
		class1.init();
		class1.testNoEmployees();
		class1.testEmployeesPaidIntra();
		class1.testSingleEmployee();
		class1.testEmployeeIsPaid();
		class1.testAllEmployeesArePaidArrayIntra();
		class1.testAllEmployeesArePaidArray();
		ca.liang.PayRollMockTest class2 = new ca.liang.PayRollMockTest();
		class2.init();
		class2.testNoEmployees();
		class2.testNoEmployeesIntra();
		class2.testSingleEmployee();
		class2.testEmployeeIsPaid();
		class2.testAllEmployeesArePaid1();
		class2.testAllEmployeesArePaid2();
		class2.testInteractionOrder();
		ca.liang.PayRollTest class3 = new ca.liang.PayRollTest();
		class3.init();
		class3.testNoEmployees();
		class3.testNoEmployeesIntra();
		class3.testSingleEmployee();
		class3.testEmployeeIsPaid();
		class3.testAllEmployeesArePaid();
		ca.liang.PayRollAnnotationMockTest class4 = new ca.liang.PayRollAnnotationMockTest();
		class4.init();
		class4.testNoEmployees();
		class4.testEmployeesPaidIntra();
		class4.testSingleEmployee();
		class4.testEmployeeIsPaid();
		ca.liang.PayRollArrayTest class5 = new ca.liang.PayRollArrayTest();
		class5.init();
		class5.testNoEmployees();
		class5.testEmployeesPaidIntra();
		class5.testSingleEmployee();
		class5.testEmployeeIsPaid();
		class5.testAllEmployeesArePaidArray();
	}
}