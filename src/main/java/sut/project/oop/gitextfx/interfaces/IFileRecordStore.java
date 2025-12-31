package sut.project.oop.gitextfx.interfaces;

import sut.project.oop.gitextfx.models.FileRecord;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface IFileRecordStore {
    FileRecord get(int id) throws SQLException;

    List<FileRecord> getAll() throws SQLException;

    void insert(String path, LocalDateTime created_at) throws SQLException;

    void delete(int id) throws SQLException;

}
