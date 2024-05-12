package projects.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import provided.util.DaoBase;
import projects.entity.*;
import projects.exception.DbException;

public class ProjectDao extends DaoBase {
	private static final String CATEGORY_TABLE = "category";
	private static final String MATERIAL_TABLE = "material";
	private static final String PROJECT_TABLE = "project";
	private static final String PROJECT_CATEGORY_TABLE = "project_category";
	private static final String STEP_TABLE = "step";
	
	/*
	 * Connects to sql database using the DbConnection class
	 * and uses gathered data to insert a project into the database
	 */
	public Project insertProject(Project project) {
		//@formatter:off
		String sql = ""
				+ "INSERT INTO " + PROJECT_TABLE + " "
				+"(project_name, estimated_hours, actual_hours, difficulty, notes) "
				+ "VALUES "
				+ "(?, ?, ?, ?, ?)";
		//@formatter:on
		try(Connection conn = DbConnection.getConnection()){
			startTransaction(conn);
			
			try(PreparedStatement stmt = conn.prepareStatement(sql)){
				setParameter(stmt, 1, project.getProjectName(), String.class);
				setParameter(stmt, 2, project.getEstimatedHours(), BigDecimal.class);
				setParameter(stmt, 3, project.getActualHours(), BigDecimal.class);
				setParameter(stmt, 4, project.getDifficulty(), Integer.class);
				setParameter(stmt, 5, project.getNotes(), String.class);
				
				stmt.executeUpdate();
				int projectId = getLastInsertId(conn, PROJECT_TABLE);
				
				commitTransaction(conn);
				
				project.setProjectId(projectId);
				return project;
			}catch(Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
		}  catch (SQLException e) {
			throw new DbException(e);
		}
	}

	/*
	 * 
	 */
	public List<Project> fetchAllProjects() {
		String sql = "" 
				+ "SELECT * FROM " + PROJECT_TABLE + " "
				+ "ORDER BY project_name ASC";
		try(Connection conn = DbConnection.getConnection()){
			startTransaction(conn);
			
			try(PreparedStatement stmt = conn.prepareStatement(sql)){
				try(ResultSet rs = stmt.executeQuery()){
					List<Project> projects = new LinkedList<>();
					
					while(rs.next()) {
						projects.add(extract(rs,Project.class));
					}
					return projects;
				}
			}catch(Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
		} catch (SQLException e) {
			throw new DbException(e);
		}

	}

	public Optional<Project> fetchProjectById(Integer projId) {
		String sql = "SELECT * FROM " + PROJECT_TABLE + " WHERE project_id = ?";
		
		try(Connection conn = DbConnection.getConnection()){
			startTransaction(conn);
			try(PreparedStatement stmt = conn.prepareStatement(sql)){
				Project project = null;
				setParameter(stmt, 1, projId, Integer.class);
				try(ResultSet rs = stmt.executeQuery()){
					if(rs.next()){
						project = extract(rs, Project.class);
					}
				}
				
				if(Objects.nonNull(project)) {
					project.getMaterials().addAll(fetchMaterialsForProject(conn, projId));
					project.getSteps().addAll(fetchStepsForProject(conn, projId));
					project.getCategories().addAll(fetchCategoriesForProject(conn,projId));
				}
				
				commitTransaction(conn);
				
				return Optional.ofNullable(project);
			}catch(Exception e) {
				rollbackTransaction(conn);
				throw new DbException(e);
			}
			
		}  catch (SQLException e) {
			throw new DbException(e);
		}
	}

	private List<Material> fetchMaterialsForProject(Connection conn, Integer projId) throws SQLException{
		//@formatter:off
		String sql = ""
				+ "SELECT m.* FROM " + MATERIAL_TABLE + " m "
				+ "WHERE project_id = ?";
		//@formatter:on
		
		try(PreparedStatement stmt = conn.prepareStatement(sql)){
			setParameter(stmt, 1, projId, Integer.class);
			
			try(ResultSet rs = stmt.executeQuery()){
				List<Material> materials = new LinkedList<>();
				
				while(rs.next()) {
					materials.add(extract(rs, Material.class));
				}
				
				return materials;
			}
		}
	}
	
	private List<Step> fetchStepsForProject(Connection conn, Integer projId) throws SQLException{
		//@formatter:off
		String sql = ""
				+ "SELECT s.* FROM " + STEP_TABLE + " s "
				+ "WHERE project_id = ?";
		//@formatter:on
		
		try(PreparedStatement stmt = conn.prepareStatement(sql)){
			setParameter(stmt, 1, projId, Integer.class);
			
			try(ResultSet rs = stmt.executeQuery()){
				List<Step> steps = new LinkedList<>();
				
				while(rs.next()) {
					steps.add(extract(rs, Step.class));
				}
				
				return steps;
			}
		}
	}
	
	private List<Category> fetchCategoriesForProject(Connection conn, Integer projId) throws SQLException{
		//@formatter:off
		String sql = ""
				+ "SELECT c.* FROM " + CATEGORY_TABLE + " c "
				+ "Join " + PROJECT_CATEGORY_TABLE + " pc USING (category_id) "
				+ "WHERE project_id = ?";
		//@formatter:on
		
		try(PreparedStatement stmt = conn.prepareStatement(sql)){
			setParameter(stmt, 1, projId, Integer.class);
			
			try(ResultSet rs = stmt.executeQuery()){
				List<Category> categories = new LinkedList<>();
				
				while(rs.next()) {
					categories.add(extract(rs, Category.class));
				}
				
				return categories;
			}
		}
	}
}







