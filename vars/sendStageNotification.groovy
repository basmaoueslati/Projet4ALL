def call(Map args) {
    def recipient = args.recipient
    def stageName = args.stageName
    
    // Status styling
    def statusColor = currentBuild.currentResult == 'SUCCESS' ? '#27ae60' : '#e74c3c'
    def statusIcon = currentBuild.currentResult == 'SUCCESS' ? '✅' : '❌'
    
    // Failure details
    def failureReport = ""
    if (currentBuild.currentResult == 'FAILURE') {
        try {
            def rawLog = sh(
                script: "curl -s ${env.BUILD_URL}consoleText | tail -n 200",
                returnStdout: true
            ).trim()
            
            def errorLines = rawLog.readLines()
                .findAll { line -> line =~ /(ERROR|FAILED|Exception|at .+\.java:\d+)/ }
                .take(50) // Limit to 50 error lines
                .join('\n')
                
            failureReport = """
                <div style="margin-top:20px;background:#fef6f6;border-left:4px solid #e74c3c;padding:10px;">
                    <h3 style="color:#e74c3c;margin-top:0;">🚨 Failure Analysis</h3>
                    <div style="font-family:monospace;white-space:pre-wrap;overflow-x:auto;">
                        ${errorLines ?: 'No specific error patterns detected'}
                    </div>
                    <p style="margin-bottom:0;">
                        <a href="${env.BUILD_URL}consoleFull" style="color:#3498db;">
                            View complete build log →
                        </a>
                    </p>
                </div>
            """
        } catch (Exception e) {
            failureReport = """
                <div style="color:#e74c3c;">
                    Failed to generate error report: ${e.message}
                </div>
            """
        }
    }

    emailext(
        subject: "${currentBuild.currentResult}: ${env.JOB_NAME} [${env.BUILD_NUMBER}] - ${stageName}",
        body: """<div style="font-family:Arial,sans-serif;max-width:800px;">
               <h2 style="color:${statusColor};border-bottom:1px solid #eee;padding-bottom:8px;">
                   ${statusIcon} ${env.JOB_NAME} - ${stageName}
               </h2>
               <div style="margin:15px 0;">
                   <p><b>Project:</b> ${env.JOB_NAME}</p>
                   <p><b>Stage:</b> ${stageName}</p>
                   <p><b>Build #:</b> ${env.BUILD_NUMBER}</p>
                   <p><b>Status:</b> <span style="color:${statusColor};font-weight:bold;">
                       ${currentBuild.currentResult}
                   </span></p>
                   <p><b>Duration:</b> ${currentBuild.durationString.replace(' and counting', '')}</p>
               </div>
               ${failureReport}
               <div style="margin-top:20px;color:#7f8c8d;font-size:0.9em;border-top:1px solid #eee;padding-top:10px;">
                   <a href="${env.BUILD_URL}" style="color:#3498db;text-decoration:none;">
                       Open in Jenkins
                   </a> • ${new Date().format("yyyy-MM-dd HH:mm")}
               </div>
               </div>""",
        to: recipient,
        mimeType: 'text/html',
        replyTo: 'no-reply@yourcompany.com'
    )
}
