package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Gender;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link StudentDao} (table "estudiantes"). */
public class StudentDaoImpl implements StudentDao {

    private static final String SELECT_BASE =
            "SELECT id_estudiante, tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
                    + "primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, "
                    + "correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado "
                    + "FROM estudiantes";

    private static final String INSERT =
            "INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
                    + "primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, "
                    + "correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String ORDER = " ORDER BY primer_apellido, segundo_apellido, primer_nombre";

    private final DatabaseConnection database;

    public StudentDaoImpl(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Student save(Student student) {
        try (Connection connection = database.getConnection()) {
            return insert(connection, student);
        } catch (SQLException e) {
            throw fail("guardar el estudiante", e);
        }
    }

    @Override
    public Student registerWithGuardian(Student student, Guardian guardian) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<Guardian> existing = GuardianDaoImpl.findByDocument(connection, guardian.getDocumentNumber());
                if (existing.isPresent()) {
                    guardian.setId(existing.get().getId());
                } else {
                    GuardianDaoImpl.insert(connection, guardian);
                }
                insert(connection, student);
                linkGuardian(connection, student.getId(), guardian.getId());
                connection.commit();
                return student;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("registrar el estudiante con su acudiente", e);
        }
    }

    @Override
    public Optional<Student> findById(Integer id) {
        return findOne(SELECT_BASE + " WHERE id_estudiante = ?", String.valueOf(id), true);
    }

    @Override
    public Optional<Student> findByDocument(String documentNumber) {
        return findOne(SELECT_BASE + " WHERE numero_documento = ?", documentNumber, false);
    }

    @Override
    public List<Student> findAll() {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BASE + ORDER);
             ResultSet rs = statement.executeQuery()) {
            return mapAll(rs);
        } catch (SQLException e) {
            throw fail("listar los estudiantes", e);
        }
    }

    @Override
    public List<Student> searchByName(String term) {
        String sql = SELECT_BASE + " WHERE CONCAT_WS(' ', primer_nombre, segundo_nombre, primer_apellido, "
                + "segundo_apellido) LIKE ?" + ORDER;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + term + "%");
            try (ResultSet rs = statement.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw fail("buscar estudiantes por nombre", e);
        }
    }

    /** Updates the contact and health data (identity data is intentionally not editable). */
    @Override
    public boolean update(Student student) {
        String sql = "UPDATE estudiantes SET direccion = ?, telefono = ?, correo_electronico = ?, tipo_sangre = ?, "
                + "alergias = ?, condiciones_medicas = ? WHERE id_estudiante = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getAddress());
            statement.setString(2, student.getPhone());
            statement.setString(3, student.getEmail());
            statement.setString(4, student.getBloodType());
            statement.setString(5, student.getAllergies());
            statement.setString(6, student.getMedicalConditions());
            statement.setInt(7, student.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail("actualizar el estudiante", e);
        }
    }

    @Override
    public boolean updateStatus(int studentId, StudentStatus status) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?")) {
            statement.setString(1, status.getDbValue());
            statement.setInt(2, studentId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail("cambiar el estado del estudiante", e);
        }
    }

    // ---------------------------------------------------------------- private helpers

    private Optional<Student> findOne(String sql, String parameter, boolean numeric) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (numeric) {
                statement.setInt(1, Integer.parseInt(parameter));
            } else {
                statement.setString(1, parameter);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw fail("consultar el estudiante", e);
        }
    }

    private Student insert(Connection connection, Student student) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, student.getDocumentType().getDbValue());
            statement.setString(2, student.getDocumentNumber());
            statement.setString(3, student.getFirstName());
            statement.setString(4, student.getMiddleName());
            statement.setString(5, student.getFirstSurname());
            statement.setString(6, student.getSecondSurname());
            statement.setDate(7, JdbcUtil.toSqlDate(student.getBirthDate()));
            statement.setString(8, student.getGender().getDbValue());
            statement.setString(9, student.getAddress());
            statement.setString(10, student.getPhone());
            statement.setString(11, student.getEmail());
            statement.setString(12, student.getBloodType());
            statement.setString(13, student.getAllergies());
            statement.setString(14, student.getMedicalConditions());
            statement.setDate(15, JdbcUtil.toSqlDate(student.getAdmissionDate()));
            statement.setString(16, student.getStatus().getDbValue());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    student.setId(keys.getInt(1));
                }
            }
            return student;
        }
    }

    private void linkGuardian(Connection connection, int studentId, int guardianId) throws SQLException {
        String sql = "INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
                + "es_contacto_emergencia) VALUES (?, ?, TRUE, TRUE)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, guardianId);
            statement.executeUpdate();
        }
    }

    private List<Student> mapAll(ResultSet rs) throws SQLException {
        List<Student> students = new ArrayList<>();
        while (rs.next()) {
            students.add(mapRow(rs));
        }
        return students;
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id_estudiante"));
        student.setDocumentType(DocumentType.fromDbValue(rs.getString("tipo_documento")));
        student.setDocumentNumber(rs.getString("numero_documento"));
        student.setFirstName(rs.getString("primer_nombre"));
        student.setMiddleName(rs.getString("segundo_nombre"));
        student.setFirstSurname(rs.getString("primer_apellido"));
        student.setSecondSurname(rs.getString("segundo_apellido"));
        student.setBirthDate(JdbcUtil.getLocalDate(rs, "fecha_nacimiento"));
        student.setGender(Gender.fromDbValue(rs.getString("genero")));
        student.setAddress(rs.getString("direccion"));
        student.setPhone(rs.getString("telefono"));
        student.setEmail(rs.getString("correo_electronico"));
        student.setBloodType(rs.getString("tipo_sangre"));
        student.setAllergies(rs.getString("alergias"));
        student.setMedicalConditions(rs.getString("condiciones_medicas"));
        student.setAdmissionDate(JdbcUtil.getLocalDate(rs, "fecha_ingreso"));
        student.setStatus(StudentStatus.fromDbValue(rs.getString("estado")));
        return student;
    }

    private DataAccessException fail(String action, SQLException cause) {
        AppLogger.error("StudentDao: failed to " + action, cause);
        return new DataAccessException("Error de base de datos al " + action + ".", cause);
    }
}
