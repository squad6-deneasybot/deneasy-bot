package com.squad6.deneasybot;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseConnectionTester implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=============================================");
        System.out.println("INICIANDO TESTE DE CONEXÃO COM O BANCO DE DADOS...");

        try (Connection connection = dataSource.getConnection()) {
            if (connection != null) {
                System.out.println(">>> SUCESSO! Conexão com o banco de dados foi estabelecida.");
            } else {
                System.out.println(">>> FALHA! Não foi possível conectar ao banco de dados.");
            }
        } catch (SQLException e) {
            System.err.println(">>> ERRO CRÍTICO AO CONECTAR AO BANCO DE DADOS!");
            System.err.println(">>> Causa: " + e.getMessage());
        }
        System.out.println("=============================================");
    }
}