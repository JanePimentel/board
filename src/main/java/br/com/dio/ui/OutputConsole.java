package br.com.dio.ui;

public class OutputConsole implements OutputInterface{
    @Override
    public void print(String message) {
        System.out.println(message);
    }

    @Override
    public void error(String message) {
        System.out.println(message);
    }
}
