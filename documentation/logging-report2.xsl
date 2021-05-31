<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="directory" required="yes" as="xs:string"></xsl:param>
	<xsl:template match="/"></xsl:template>
	<html>
		<body>
			<h2>Detailed log messages report</h2>
			<table border="1">
				<tr bgcolor="#9acd32">
					<th>ID</th>
					<th>Message</th>
					<th>Level</th>
					<th>Description</th>
				</tr>
				 <xsl:apply-templates select="main"/>
				</table>
		</body>
	</html>
				<xsl:template match="main">
					<xsl:for-each
						select="collection(concat($directory,'?select=*.xml;recurse=yes'))">
						<xsl:result-document href="{......}">
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
				 		</xsl:result-document>
					</xsl:for-each>
				</xsl:template>
			
</xsl:stylesheet>