{{- define "invoice-api.name" -}}
{{- .Chart.Name -}}
{{- end }}

{{- define "invoice-api.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "invoice-api.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end }}

{{- define "invoice-api.labels" -}}
app.kubernetes.io/name: {{ include "invoice-api.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/environment: {{ .Values.environment | quote }}
{{- end }}
