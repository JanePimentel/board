package br.com.dio.ui;

import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardColumnKindEnum;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.BoardService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.*;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");
    private final OutputInterface output;

    public MainMenu(OutputInterface output) {
        this.output = output;
    }

    public void execute() throws SQLException {
        output.print("Bem-vindo ao gerenciador de boards, escolha a opção desejada");
        var option = -1;
        while (true) {
            output.print("1 - Criar um novo board");
            output.print("2 - Selecionar um board existente");
            output.print("3 - Excluir um board");
            output.print("4 - Sair");
            option = scanner.nextInt();
            switch (option) {
                case 1 -> createBoard();
                case 2 -> selectBoard();
                case 3 -> deleteBoard();
                case 4 -> System.exit(0);
                default -> output.error("Opção inválida, informe uma opção do menu");
            }
        }
    }

    private void createBoard() throws SQLException {
        var entity = new BoardEntity();
        output.print("Informe o nome do seu board:");
        entity.setName(scanner.next());

        output.print("Seu board terá colunas além das 3 padrões? Se sim informe quantas, senão digite '0':");
        var additionalColumns = scanner.nextInt();

        List<BoardColumnEntity> columns = new ArrayList<>();

        output.print("Informe o nome da coluna inicial do board:");
        var initialColumnName = scanner.next();
        var initialColumn = createColumn(initialColumnName, INITIAL, 0);
        columns.add(initialColumn);

        for (int i = 0; i < additionalColumns; i++) {
            output.print("Informe o nome da coluna de tarefa pendente:");
            var pendingColumnName = scanner.next();
            var pendingColumn = createColumn(pendingColumnName, PENDING, i + 1);
            columns.add(pendingColumn);
        }

        output.print("Informe o nome da coluna final:");
        var finalColumnName = scanner.next();
        var finalColumn = createColumn(finalColumnName, FINAL, additionalColumns + 1);
        columns.add(finalColumn);

        output.print("Informe o nome da coluna de cancelamento:");
        var cancelColumnName = scanner.next();
        var cancelColumn = createColumn(cancelColumnName, CANCEL, additionalColumns + 2);
        columns.add(cancelColumn);

        entity.setBoardColumns(columns);
        try (var connection = getConnection()) {
            var service = new BoardService(connection);
            service.insert(entity);
            output.print("Board criado com sucesso!");
        }
    }

    private void selectBoard() throws SQLException {
        output.print("Informe o id do board que deseja selecionar:");
        var id = scanner.nextLong();
        try (var connection = getConnection()) {
            var queryService = new BoardQueryService(connection);
            var optional = queryService.findById(id);
            optional.ifPresentOrElse(
                    board -> new BoardMenu(board, output).execute(),
                    () -> output.error("Não foi encontrado um board com id %s".formatted(id))
            );
        }
    }

    private void deleteBoard() throws SQLException {
        output.print("Informe o id do board que será excluído:");
        var id = scanner.nextLong();
        try (var connection = getConnection()) {
            var service = new BoardService(connection);
            if (service.delete(id)) {
                output.print("O board %s foi excluído.".formatted(id));
            } else {
                output.error("Não foi encontrado um board com id %s".formatted(id));
            }
        }
    }

    private BoardColumnEntity createColumn(final String name, final BoardColumnKindEnum kind, final int order) {
        var boardColumn = new BoardColumnEntity();
        boardColumn.setName(name);
        boardColumn.setKind(kind);
        boardColumn.setOrder(order);
        return boardColumn;
    }
}
