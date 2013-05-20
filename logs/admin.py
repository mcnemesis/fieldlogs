from django.contrib import admin
from django.contrib.sites.management import Site
from logs.models import *
from django.http import HttpResponseRedirect

#we may not need this...
try:
    admin.site.unregister(Site)
except:
    pass

class LogAdmin(admin.ModelAdmin):
    list_display = ('id','created','view_age','subject','nationality','location', 'comment','view_signature','view_photo','barcode','view_location',)
    list_filter = ('nationality','created',)
    actions = ['export_logs',]
    def export_logs(self, request, queryset):
        selected = request.POST.getlist(admin.ACTION_CHECKBOX_NAME)
        return HttpResponseRedirect("%s/admin/fieldlogs/log/export?limit=%s" % (BASE_URL,",".join(selected)))
    export_logs.short_description = 'Export Logs'

admin.site.register(Log,LogAdmin)
