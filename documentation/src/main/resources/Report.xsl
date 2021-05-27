<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<body>
				<h2>Detailed log messages report for <xsl:value-of select="report/@class"/></h2>
				<table border="1">
					<tr bgcolor="#9acd32">
						<th>ID</th>
						<th>Message</th>
						<th>Level</th>
						<th>Description</th>
					</tr>
					<xsl:for-each select="report/logs/log">
						<tr>
							<td>
								<xsl:value-of select="id" />
							</td>
							<td>
								<xsl:value-of select="message" />
							</td>
							<td>
								<xsl:value-of select="level" />
							</td>
							<td>
								<xsl:value-of select="description" />
							</td>
						</tr>
					</xsl:for-each>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>