package sut.project.oop.gitextfx.interfaces;

import sut.project.oop.gitextfx.models.FileRecord;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface IFileRecordStore {
    FileRecord getFileRecord(int id) throws SQLException;

    List<FileRecord> getAllFileRecords() throws SQLException;

    long insertNewFileRecord(String path, LocalDateTime created_at, int non_delta_interval) throws SQLException;

    void deleteFileRecord(int id) throws SQLException;

    void updateLastedEdit(int id, LocalDateTime time) throws SQLException;

}
