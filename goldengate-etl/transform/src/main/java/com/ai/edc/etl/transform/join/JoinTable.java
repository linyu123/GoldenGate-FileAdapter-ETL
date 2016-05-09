package com.ai.edc.etl.transform.join;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ai.edc.common.utils.CollectionUtil;
import com.ai.edc.etl.transform.dbassist.AutoScalingRowDataLoader;
import com.ai.edc.etl.transform.dbmodel.AutoScalingRowData;
import com.ai.edc.etl.transform.dbmodel.DBModel;

@Component
public class JoinTable implements IJoin {
	public AutoScalingRowData join(Connection statisticsConn, DBModel model,
			AutoScalingRowData data) throws SQLException {
		if (!JoinConfigScript.hasScript(model.getTableName())) {
			return data;
		}

		String pkColumnName = JoinConfigScript.getColumn4FindTargetDefine(model
				.getTableName());
		String pk = data.getColumnValue(pkColumnName);

		ArrayList<String> transformRules = JoinConfigScript
				.getTransformDefine(model.getTableName());
		if (CollectionUtil.isEmpty(transformRules)) {
			throw new TableJoinExcetpion("table[" + model.getTableName()
					+ "] transform rule is not define.");
		}

		String finalTargetTableName = null;
		Map<String, String> columnNameMapping = new HashMap<String, String>();
		for (String transformRule : transformRules) {
			String[] rules = transformRule.split("->");
			if (rules.length != 2) {
				throw new TableJoinExcetpion(
						"table["
								+ model.getTableName()
								+ "] transform rule["
								+ transformRule
								+ "] is illegal. usage: column->targetTable.targetColumn");
			}
			String[] targets = rules[1].split("\\.");
			if (targets.length != 2) {
				throw new TableJoinExcetpion(
						"table["
								+ model.getTableName()
								+ "] transform rule["
								+ transformRule
								+ "] is illegal. target usage: targetTable.targetColumn");
			}
			String sourceColumnName = rules[0];
			String targetTableName = targets[0];
			String targetColumnName = targets[1];

			if (finalTargetTableName == null) {
				finalTargetTableName = targetTableName;
			} else if (!finalTargetTableName.equals(targetTableName)) {
				throw new TableJoinExcetpion(
						"table["
								+ model.getTableName()
								+ "] transform rule["
								+ transformRule
								+ "] is illegal. targetTable is changed. prevTargetTable="
								+ finalTargetTableName);
			}

			columnNameMapping.put(sourceColumnName, targetColumnName);
		}

		AutoScalingRowData targetData = AutoScalingRowDataLoader.load(
				statisticsConn, finalTargetTableName, pk);
		
		/**
		 * join columnValue to target
		 */
		for(String sourceColumnName : columnNameMapping.keySet()){
			sourceColumnName = sourceColumnName.toUpperCase();
			String targetColumnName = columnNameMapping.get(sourceColumnName).toUpperCase();
			targetData.setColumnValue(targetColumnName, data.getColumnValue(sourceColumnName));
		}
		return targetData;
	}
}