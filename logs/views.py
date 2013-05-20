from django.http import HttpResponse
from fieldlogs.settings import *
from logs.models import *
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render_to_response
import json
import logging
import datetime
import django_tables2 as tables
from django_tables2   import RequestConfig
from django.contrib.auth.decorators import login_required
from django.template import RequestContext
import csv

logging.basicConfig(level=LOG_LEVEL, filename=MAIN_LOG)
logger = logging.getLogger(__name__)


class ExportLogsTable(tables.Table):
    COLUMNS = ['ID','CREATED','LATITUDE','LONGITUDE','LOCATION','SUBJECT','COMMENT','NATIONALITY','BIRTHDATE','BARCODE', 'PHOTO','PHOTO_CAPTION','SIGNATURE',]
    class Meta:
        model = Log
        attrs = {"class": "paleblue"}
        exclude = ('updated',)
        sequence = ('id','created','latitude','longitude','location','subject','comment','nationality','birthdate','barcode', 'photo','photo_caption','signature',)


def j_response(payload, msg, status):
    return HttpResponse( json.dumps( {
        'payload' : payload,
        'message' : msg,
        'status' : status
        }, indent=2), status = status)

def j_error(msg):
    logger.error(msg)
    return j_response(None, msg, 403)

def j_ok(payload, msg):
    logger.info(msg)
    return j_response(payload, msg, 200)

@csrf_exempt
def api_add_log(request):
    if request.method != 'POST':
        return j_error('HTTP VERB Not Supported')


    def p(key):
        return request.POST.get(key,None) if key in request.POST else None
    def f(key):
        return request.FILES.get(key,None) if key in request.FILES else None
    def e(key):
        return True if key in request.POST else False
    def fe(key):
        return True if key in request.FILES else False

    required = ['lat','lng','sub','nat','dob']

    if reduce(lambda u,v: u and v, map(e,required), True) and fe('sig'):
        log = Log(
                latitude = float( p('lat') ),
                longitude = float( p('lng') ),
                subject = p('sub'),
                nationality = p('nat'),
                birthdate = datetime.datetime.strptime( p('dob'), '%Y-%m-%d'),
                location = p('loc'),
                comment = p('com'),
                barcode = p('code'),
                photo_caption = p('cap'),
                signature = f('sig'),
                photo = f('img')
                )
        log.save()
        return j_ok( log.to_dict(), 'Log : %s' % log.id)
    else:
        return j_error('Required: sig, %s' % ', '.join(required))


@csrf_exempt
@login_required
def export_logs(request):
    logs = None
    if 'limit' in request.GET:
        ids = map(int,request.GET['limit'].split(','))
        logs = Log.objects.filter(id__in=ids)
    else:
        filters = {}
        for filter,arg in request.GET.items():
            if filter != 'sort': #ignore django-tables2 sort keywords
                filters.update({filter:arg})
        try:
            logs = Log.objects.filter(**filters)
        except:
            logs = Log.objects.all()

    logs_table = ExportLogsTable(logs)
    RequestConfig(request).configure(logs_table)

    if request.method == 'POST':
        if 'export' in request.POST:
            kind = request.POST.get('export','csv')
            response = HttpResponse(mimetype='text/csv')
            response['Content-Disposition'] = 'attachment;filename="logs.csv"'
            #a portable way, avoiding writeheader()
            writer = csv.DictWriter(response,ExportLogsTable.COLUMNS)
            d = {}
            map(lambda i: d.update({i:i}),ExportLogsTable.COLUMNS)
            writer.writerow(d)
            #we choose to iterate over the table instead of the queryset, to exploit the possibility of custom
            #sorting criteria applied by user via django-tables2
            for row in logs_table.rows:
                writer.writerow(row.record.to_dict())

            return response


    parent_uri = "%s/admin/%s/%s/" %(BASE_URL,Log._meta.app_label,  Log._meta.module_name)
    dashboard_url = "%s/admin/" %(BASE_URL)
    return render_to_response('export.html',RequestContext(request,{
            'title' : 'Export Field Logs',
            'parent' : 'Field Logs',
            'table' : logs_table,
            'parent_url' : parent_uri,
            'dashboard_url' : dashboard_url
        }))
