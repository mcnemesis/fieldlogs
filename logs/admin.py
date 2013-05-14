from django.contrib import admin
from django.contrib.sites.management import Site
from logs.models import *


#we may not need this...
try:
    admin.site.unregister(Site)
except:
    pass

class LogAdmin(admin.ModelAdmin):
    list_display = ('id','created','view_age','subject','nationality','location', 'comment','view_signature','view_photo','barcode','view_location',)
    list_filter = ('nationality','created',)

admin.site.register(Log,LogAdmin)
