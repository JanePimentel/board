package br.com.dio.service;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.exception.CardBlockedException;
import br.com.dio.exception.CardFinishedException;
import br.com.dio.exception.EntityNotFoundException;
import br.com.dio.persistence.dao.BlockDAO;
import br.com.dio.persistence.dao.CardDAO;
import br.com.dio.persistence.entity.CardEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static br.com.dio.persistence.entity.BoardColumnKindEnum.CANCEL;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.FINAL;
import static br.com.dio.util.TransactionExecutor.*;

@AllArgsConstructor
public class CardService {

    private final Connection connection;

    public CardEntity create(final CardEntity entity) throws SQLException {
        return execute(connection, conn -> {
            var dao = new CardDAO(conn);
            dao.insert(entity);
            return entity;
        });
    }

    public void moveToNextColumn(final Long cardId, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        execute(connection, conn -> {
            var dao = new CardDAO(conn);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(cardId))
            );

            if (dto.blocked()) {
                throw new CardBlockedException("O card %s está bloqueado, é necessário desbloqueá-lo para mover".formatted(cardId));
            }

            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));

            if (currentColumn.kind().equals(FINAL)) {
                throw new CardFinishedException("O card já foi finalizado");
            }

            var nextColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.order() == currentColumn.order() + 1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card está cancelado"));

            dao.moveToColumn(nextColumn.id(), cardId);
            return null;
        });
    }

    public void cancel(final Long cardId, final Long cancelColumnId, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        execute(connection, conn -> {
            var dao = new CardDAO(conn);
            var optional = dao.findById(cardId);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(cardId))
            );

            if (dto.blocked()) {
                throw new CardBlockedException("O card %s está bloqueado, é necessário desbloqueá-lo para mover".formatted(cardId));
            }

            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));

            if (currentColumn.kind().equals(FINAL)) {
                throw new CardFinishedException("O card já foi finalizado");
            }

            boardColumnsInfo.stream()
                    .filter(bc -> bc.order() == currentColumn.order() + 1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("O card está cancelado"));

            dao.moveToColumn(cancelColumnId, cardId);
            return null;
        });
    }

    public void block(final Long id, final String reason, final List<BoardColumnInfoDTO> boardColumnsInfo) throws SQLException {
        execute(connection, conn -> {
            var dao = new CardDAO(conn);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );

            if (dto.blocked()) {
                throw new CardBlockedException("O card %s já está bloqueado".formatted(id));
            }

            var currentColumn = boardColumnsInfo.stream()
                    .filter(bc -> bc.id().equals(dto.columnId()))
                    .findFirst()
                    .orElseThrow();

            if (currentColumn.kind().equals(FINAL) || currentColumn.kind().equals(CANCEL)) {
                throw new IllegalStateException("O card está em uma coluna do tipo %s e não pode ser bloqueado"
                        .formatted(currentColumn.kind()));
            }

            var blockDAO = new BlockDAO(conn);
            blockDAO.block(reason, id);
            return null;
        });
    }

    public void unblock(final Long id, final String reason) throws SQLException {
        execute(connection, conn -> {
            var dao = new CardDAO(conn);
            var optional = dao.findById(id);
            var dto = optional.orElseThrow(
                    () -> new EntityNotFoundException("O card de id %s não foi encontrado".formatted(id))
            );

            if (!dto.blocked()) {
                throw new CardBlockedException("O card %s não está bloqueado".formatted(id));
            }

            var blockDAO = new BlockDAO(conn);
            blockDAO.unblock(reason, id);
            return null;
        });
    }
}
