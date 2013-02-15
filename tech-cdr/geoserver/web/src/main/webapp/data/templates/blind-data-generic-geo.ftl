WITH MEMBER [Measures].[allBlindData] AS '${measureList}' SELECT { [Measures].[uuid_area], [Measures].[allBlindData] } ON 0, { [${geoDimension}].Children } ON 1 from [${cube}]
 
