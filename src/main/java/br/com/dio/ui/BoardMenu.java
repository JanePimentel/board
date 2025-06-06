package br.com.dio.ui;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.persistence.entity.CardEntity;
import br.com.dio.service.BoardColumnQueryService;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.CardQueryService;
import br.com.dio.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    private final BoardEntity entity;
    private final OutputInterface output;

    public void execute() {
        try {
            output.print("Bem-vindo ao board %s, selecione a operação desejada".formatted(entity.getId()));
            var option = -1;
            while (option != 9) {
                output.print("1 - Criar um card");
                output.print("2 - Mover um card");
                output.print("3 - Bloquear um card");
                output.print("4 - Desbloquear um card");
                output.print("5 - Cancelar um card");
                output.print("6 - Ver board");
                output.print("7 - Ver coluna com cards");
                output.print("8 - Ver card");
                output.print("9 - Voltar para o menu anterior");
                output.print("10 - Sair");
                option = scanner.nextInt();
                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> output.print("Voltando para o menu anterior");
                    case 10 -> System.exit(0);
                    default -> output.error("Opção inválida, informe uma opção do menu");
                }
            }
        } catch (SQLException ex) {
            output.error("Erro: " + ex.getMessage());
            System.exit(0);
        }
    }

    private void createCard() throws SQLException {
        var card = new CardEntity();
        output.print("Informe o título do card:");
        card.setTitle(scanner.next());
        output.print("Informe a descrição do card:");
        card.setDescription(scanner.next());
        card.setBoardColumn(entity.getInitialColumn());
        try (var connection = getConnection()) {
            new CardService(connection).create(card);
            output.print("Card criado com sucesso!");
        }
    }

    private void moveCardToNextColumn() throws SQLException {
        output.print("Informe o id do card que deseja mover para a próxima coluna:");
        var cardId = scanner.nextLong();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try (var connection = getConnection()) {
            new CardService(connection).moveToNextColumn(cardId, boardColumnsInfo);
            output.print("Card movido com sucesso.");
        } catch (RuntimeException ex) {
            output.error(ex.getMessage());
        }
    }

    private void blockCard() throws SQLException {
        output.print("Informe o id do card que será bloqueado:");
        var cardId = scanner.nextLong();
        output.print("Informe o motivo do bloqueio:");
        var reason = scanner.next();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try (var connection = getConnection()) {
            new CardService(connection).block(cardId, reason, boardColumnsInfo);
            output.print("Card bloqueado com sucesso.");
        } catch (RuntimeException ex) {
            output.error(ex.getMessage());
        }
    }

    private void unblockCard() throws SQLException {
        output.print("Informe o id do card que será desbloqueado:");
        var cardId = scanner.nextLong();
        output.print("Informe o motivo do desbloqueio:");
        var reason = scanner.next();
        try (var connection = getConnection()) {
            new CardService(connection).unblock(cardId, reason);
            output.print("Card desbloqueado com sucesso.");
        } catch (RuntimeException ex) {
            output.error(ex.getMessage());
        }
    }

    private void cancelCard() throws SQLException {
        output.print("Informe o id do card que deseja cancelar:");
        var cardId = scanner.nextLong();
        var cancelColumn = entity.getCancelColumn();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();
        try (var connection = getConnection()) {
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnsInfo);
            output.print("Card cancelado com sucesso.");
        } catch (RuntimeException ex) {
            output.error(ex.getMessage());
        }
    }

    private void showBoard() throws SQLException {
        try (var connection = getConnection()) {
            var optional = new BoardQueryService(connection).showBoardDetails(entity.getId());
            optional.ifPresent(b -> {
                output.print("Board [%s, %s]".formatted(b.id(), b.name()));
                b.columns().forEach(c ->
                        output.print("Coluna [%s] tipo: [%s] tem %s cards".formatted(c.name(), c.kind(), c.cardsAmount()))
                );
            });
        }
    }

    private void showColumn() throws SQLException {
        var columnsIds = entity.getBoardColumns().stream().map(BoardColumnEntity::getId).toList();
        var selectedColumnId = -1L;
        while (!columnsIds.contains(selectedColumnId)) {
            output.print("Escolha uma coluna do board %s pelo id".formatted(entity.getName()));
            entity.getBoardColumns().forEach(c ->
                    output.print("%s - %s [%s]".formatted(c.getId(), c.getName(), c.getKind()))
            );
            selectedColumnId = scanner.nextLong();
        }
        try (var connection = getConnection()) {
            var column = new BoardColumnQueryService(connection).findById(selectedColumnId);
            column.ifPresent(co -> {
                output.print("Coluna %s tipo %s".formatted(co.getName(), co.getKind()));
                co.getCards().forEach(ca ->
                        output.print("Card %s - %s\nDescrição: %s"
                                .formatted(ca.getId(), ca.getTitle(), ca.getDescription()))
                );
            });
        }
    }

    private void showCard() throws SQLException {
        output.print("Informe o id do card que deseja visualizar:");
        var selectedCardId = scanner.nextLong();
        try (var connection = getConnection()) {
            new CardQueryService(connection).findById(selectedCardId)
                    .ifPresentOrElse(
                            c -> {
                                output.print("Card %s - %s".formatted(c.id(), c.title()));
                                output.print("Descrição: %s".formatted(c.description()));
                                output.print(c.blocked()
                                        ? "Está bloqueado. Motivo: " + c.blockReason()
                                        : "Não está bloqueado");
                                output.print("Já foi bloqueado %s vezes".formatted(c.blocksAmount()));
                                output.print("Está atualmente na coluna %s - %s".formatted(c.columnId(), c.columnName()));
                            },
                            () -> output.error("Não existe um card com o id %s".formatted(selectedCardId))
                    );
        }
    }
}
