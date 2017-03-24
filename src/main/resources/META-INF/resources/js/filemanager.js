/**
 *  Filemanager JS core
 *
 *  filemanager.jsl
 *
 *  @license  MIT License
 *  @author    Jason Huck - Core Five Labs
 *  @author    Simon Georget <simon (at) linea21 (dot) com>
 *  @copyright  Authors
 */

function fileManager(_initialData, _$beaconTable, _startURI, _folderPath,
                     _canWriteFlag, _totalDataCount, lg, contextPath,
                     useDefaultLogin)
{
// function to retrieve GET params
  $.urlParam = function (name)
  {
    var results = new RegExp('[\\?&]' + name +
                             '=([^&#]*)').exec(window.location.href);
    return results ? results[1] : 0;
  };

  /*---------------------------------------------------------
   Setup, Layout, and Status Functions
   ---------------------------------------------------------*/

// We retrieve config settings from filemanager.config.js
  var loadConfigFile = function (type)
  {
    var json = null;
    type = (typeof type === "undefined") ? "user" : type;

    var url;

    if (type == 'user')
    {
      if ($.urlParam('config') != 0)
      {
        url = contextPath + 'scripts/' + $.urlParam('config');
        userconfig = $.urlParam('config');
      }
      else
      {
        url = contextPath + 'scripts/filemanager.config.json';
        userconfig = 'filemanager.config.json';
      }
    }
    else
    {
      url = contextPath + 'scripts/filemanager.config.default.json';
    }

    $.ajax({
             'async': false,
             'url': url,
             'dataType': "json",
             cache: false,
             'success': function (data)
             {
               json = data;
             }
           });
    return json;
  };

// loading default configuration file
  var configd = loadConfigFile('default');
// loading user configuration file
  var config = loadConfigFile();
// we remove version from user config file
  if (config !== null)
  {
    delete config.version;
  }

  // we merge default config and user config file
  config = $.extend({}, configd, config);

  if (config.options.logger)
  {
    var start = new Date().getTime();
  }

  var ROW_SELECT_TYPE = "row";
  var lockedIcon =
    "<span class=\"glyphicon glyphicon-lock\"></span> <a href=\"" + contextPath
    + "unlock\" title=\"Unlock to modify.\">Unlock</a>";

  // Used for controlling button bar function
  var multiSelectSelector = ".multi-select-function-container";
  var multiSelectWritableSelector = ".multi-select-function-container-writable";
  var multiSelectClass = ".multi-select-function";
  var multiSelectWritableClass = ".multi-select-function-writable";
  var isWritableFlagIndex = 13;

  var stringUtil = new org.opencadc.StringUtil();
  var url = contextPath + config.options.pageConnector + _folderPath;
  var defaultPageSize = 400;

  var requestData = {};

  requestData.pageSize = defaultPageSize;

  if (_startURI != "")
  {
    requestData.startURI = encodeURIComponent(_startURI);
  }

// Options for alert, prompt, and confirm dialogues.
  $.prompt.setDefaults({
                         overlayspeed: 'fast',
                         show: 'fadeIn',
                         hide: 'fadeOut',
                         opacity: 0.4,
                         persistent: false
                       });

  var selectInput = {
    style: 'os',
    selector: 'td:first-child.select-checkbox'
  };

  var $fileInfo = $("#fileInfo");
  var fileRoot;
  var baseURL;
  var fullexpandedFolder;
  var expandedFolder;
  var $dt = _$beaconTable.DataTable(
    {
      data: _initialData,
      language: {
        search: "_INPUT_",
        searchPlaceholder: "Search Name..."
      },
      dom: "<'row beacon-info-row'<'col-sm-12'<'progress active'<'beacon-progress progress-bar progress-bar-info progress-bar-striped'>><'quota'>i>>"
           + "<'row'<'col-sm-12'tr>>",
      loading: true,
      processing: true,
      deferRender: true,
      scrollY: "60vh",
      lengthChange: false,
      scrollCollapse: true,
      scroller: true,
      columnDefs: [
        {
          "targets": 0,
          "orderable": false,
          "className": 'select-checkbox',
          "searchable": false,
          "render": function (data, type, full)
          {
            var renderedValue;

            if (full.length > 10)
            {
              var canReadFlag = (full[12] === "true");

              if (canReadFlag === true)
              {
                var lockedFlag = (full[7] === "true");

                renderedValue = lockedFlag
                  ? lockedIcon : data;
              }
              else
              {
                renderedValue = data;
              }

            }
            else
            {
              renderedValue = data;
            }

            return renderedValue;
          }
        },
        {
          "targets": 1,
          "render": function (data, type, full)
          {
            if (full.length > 10)
            {
              var itemNameDisplay =
                '<span class="glyphicon ' + full[8] + '"></span>&nbsp;&nbsp;';

              if (full[12] === "true")
              {
                itemNameDisplay +=
                  '<a href="' + full[11] + '">' + data + '</a>';
              }
              else
              {
                itemNameDisplay += data;
              }

              // if isWritable bit is true, provide edit icon
              if (full[13] === "true")
              {
                // Add data references to icon so they can be used to populate
                // the edit prompt
                var editIcon = '<span class="glyphicon glyphicon-pencil"><a href="' +
                               contextPath +
                               'update" title="Edit permissions." ' +
                               'readable="' + full[6] +
                               '" path="' + full[9] +
                               '" readGroup="' + full[5] +
                               '" writeGroup="' + full[4] +
                               '" itemName="' + data +
                               '" ></a></span>';
                itemNameDisplay += editIcon;
              }

              return itemNameDisplay;
            }
            else
            {
              return data;
            }
          }
        },
        {
          "targets": 2,
          "type": "file-size",
          "searchable": false
        },
        {
          "targets": 5,
          "searchable": false,
          "render": function (data, type, full)
          {
            var renderedValue;

            if (full.length > 9)
            {
              // Column [6] is the public flag.
              renderedValue = (full[6] === "true")
                ? lg.public : data;
            }
            else
            {
              renderedValue = data;
            }

            return renderedValue;
          }
        },
        {
          "targets": [3, 4],
          "searchable": false
        },
        {
          "targets": [6],
          "searchable": false,
          "render": function (data, type, full)
          {
            return full[14];
          }
        }
      ],
      select: selectInput,
      order: [[1, 'asc']]
    });

  // Setup the Progress Bar.
  $("div.beacon-progress").attr("role", "progressbar")
    .attr("aria-valuenow", _totalDataCount + "")
    .attr("aria-valuemin", _totalDataCount + "")
    .attr("aria-valuemax", _totalDataCount + "");
  // .text(lg.loading_data);


  var toggleButtonSet = function (_disabledFlag, selector, selectClass)
  {
    var $selectorContainers = $(selector);

    // These elements will toggle regardless of permissions
    var $selectorFunctions =
      $selectorContainers.find(selectClass);

    if (_disabledFlag === true)
    {
      $selectorContainers.addClass("disabled");
      $selectorFunctions.addClass("disabled");
    }
    else
    {
      $selectorContainers.removeClass("disabled");
      $selectorFunctions.removeClass("disabled");
    }

  };

  var toggleMultiFunctionButtons = function (_disabledFlag, writable)
  {
    toggleButtonSet(_disabledFlag, multiSelectSelector, multiSelectClass);
    if (writable === true)
    {
      toggleButtonSet(_disabledFlag, multiSelectWritableSelector, multiSelectWritableClass);
    }
  };

  var enableMultiFunctionButtons = function (writable)
  {
    toggleMultiFunctionButtons(false, writable);
  };

  var disableMultiFunctionButtons = function ()
  {
    // Passing in 'true' to writable bit here because
    // it doesn't matter when it comes to disabling the buttons: off is off!
    toggleMultiFunctionButtons(true, true);
  };


  var isSelectionWritable = function (tableRows)
  {
    if (tableRows.count() > 0)
    {
      // check isWritable for all selected rows
      var writable = true;
      for (var i = 0; i < tableRows.count(); i++)
      {
        if (tableRows.data()[i][isWritableFlagIndex] === "false")
        {
          writable = false;
          break;
        }
      }
      return writable;
    }
  };

  $dt.on("select", function (event, dataTablesAPI, type)
  {
    if (type === ROW_SELECT_TYPE)
    {
      var selectedRows = $dt.rows({selected: true});
      enableMultiFunctionButtons(isSelectionWritable(selectedRows));
    }
  });

  $dt.on("draw.dtSelect.dt select.dtSelect.dt deselect.dtSelect.dt",
         function ()
         {
           var selectedRows = $dt.rows({selected: true});
           enableMultiFunctionButtons(isSelectionWritable(selectedRows));
         });

  $dt.on("info.dt",
         function ()
         {
           disableMultiFunctionButtons();
         });


  $dt.on("deselect", function (event, dataTablesAPI, type)
  {
    // If the indexes.length is 1, this that last item is
    // being removed (deselected).
    if ((type === ROW_SELECT_TYPE)
        && ($dt.rows({selected: true}).count() <= 0))
    {
      disableMultiFunctionButtons();
    }
  });

  /**
   * We're putting a custom search field in, so we need to
   * initialize the searching here.
   */
  $("input.dataTables_filter").on("keyup",
                                  function ()
                                  {
                                    $dt.search($(this).val()).draw();
                                  });

  var getPage = function (_data, _callback)
  {
    $.get({
            url: url,
            dataType: "text",
            data: _data
          })
      .done(function (csvData)
            {
              _callback(csvData);
            });
  };

  var loadComplete = function ()
  {
    $("div.progress").removeClass("active").children("div.beacon-progress")
      .removeClass("progress-bar-striped")
      .removeClass("progress-bar-info")
      .addClass("progress-bar-success").empty();
  };

  var load = function (_callback)
  {
    getPage(requestData, function (csvData)
    {
      _callback(csvData);
    });
  };

  var successCallback = function (csvData)
  {
    var data = $.csv.toArrays(csvData);
    var dl = data.length;

    if (dl > 0)
    {
      for (var di = 0; di < dl; di++)
      {
        var nextRow = data[di];
        $dt.row.add(nextRow);

        _startURI = nextRow[10];
      }

      $dt.draw();

      requestData.startURI = _startURI;
      getPage(requestData, successCallback);
    }
    else
    {
      loadComplete();
    }
  };

  load(successCallback);

  /**
   * Page refresh.  Used after write operations to show updated state.
   */
  var refreshPage = function ()
  {
    window.location.reload(true);
  };

// <head> included files collector
  var HEAD_included_files = [];
  var userconfig;


  /**
   * function to load a given css file into header
   * if not already included
   */
  var loadCSS = function (href)
  {
    // we check if already included
    if ($.inArray(href, HEAD_included_files) == -1)
    {
      var cssLink = $("<link rel='stylesheet' type='text/css' href='" + href
                      + "'>");
      $("head").append(cssLink);

      HEAD_included_files.push(href);
    }
  };

  /**
   * function to load a given js file into header
   * if not already included
   */
  var loadJS = function (src)
  {
    // we check if already included
    if ($.inArray(src, HEAD_included_files) == -1)
    {
      var jsLink = $("<script type='text/javascript' src='" + src + "'>");
      $("head").append(jsLink);
      HEAD_included_files.push(src);
    }
  };

  /**
   * determine path when using baseUrl and
   * setFileRoot connector function to give back
   * a valid path on selectItem calls
   *
   */
  var smartPath = function (url, path)
  {
    var a = url.split('/');
    var separator = '/' + a[a.length - 2] + '/';
    var pos = path.indexOf(separator);
    var rvalue;

    // separator is not found
    // this can happen when not set dynamically with setFileRoot function - see
    //  : https://github.com/simogeo/Filemanager/issues/354
    if (pos == -1)
    {
      rvalue = url + path;
    }
    else
    {
      rvalue = url + path.substring(pos + separator.length);
    }

    if (config.options.logger)
    {
      console.log("url : " + url + " - path : " +
                  path + " - separator : " +
                  separator + " -  pos : " + pos +
                  " - returned value : " + rvalue);
    }

    return rvalue;
  };

// Sets paths to connectors based on language selection.
  var fileConnector = contextPath + config.options.fileConnector ||
                      'connectors/' + config.options.lang + '/filemanager.' +
                      config.options.lang;

// Read capabilities from config files if exists
// else apply default settings
  var capabilities = config.options.capabilities ||
    ['select', 'download', 'rename', 'move', 'delete',
     'replace'];

// Get localized messages from file
// through culture var or from URL
  if ($.urlParam('langCode') != 0)
  {
    if (file_exists(contextPath + 'scripts/languages/' + $.urlParam('langCode')
                    + '.js'))
    {
      config.options.culture = $.urlParam('langCode');
    }
    else
    {
      var urlLang = $.urlParam('langCode').substring(0, 2);

      if (file_exists(contextPath + 'scripts/languages/' + urlLang + '.js'))
      {
        config.options.culture = urlLang;
      }
    }
  }

// Forces columns to fill the layout vertically.
// Called on initial page load and on resize.
  var setDimensions = function ()
  {
    var bheight = 53, $uploader = $('#uploader');

    if ($.urlParam('CKEditorCleanUpFuncNum'))
    {
      bheight += 60;
    }

    var newH = ($uploader && ($uploader.length > 0))
      ? ($(window).height() - $uploader.height() - $uploader.offset().top -
         bheight)
      : ($(window).height() - bheight);
    $fileInfo.height(newH);
  };

// Display Min Path
  var displayPath = function (path, reduce)
  {
    reduce = (typeof reduce === "undefined");

    if (config.options.showFullPath == false)
    {
      // if a "displayPathDecorator" function is defined, use it to decorate
      // path
      if ('function' === typeof displayPathDecorator)
      {
        return displayPathDecorator(path.replace(fileRoot, "/"));
      }
      else
      {
        path = path.replace(fileRoot, "/");
        if (path.length > 50 && reduce === true)
        {
          var n = path.split("/");
          path = '/' + n[1] + '/' + n[2] + '/(...)/' + n[n.length - 2] + '/';
        }
        return path;
      }
    }
    else
    {
      return path;
    }
  };

  if (useDefaultLogin === true)
  {
    var $loginForm = $("#loginForm");

    var resetLoginFormErrors = function ()
    {
      var $loginFailContainer =
        $loginForm.find("#login_fail");

      $loginForm.removeClass("has-error");
      $loginFailContainer.text("");
    };

    $loginForm.find("input.form-control").off().change(function ()
                                                       {
                                                         resetLoginFormErrors();
                                                       });

    $loginForm.off().submit(function ()
                            {
                              var $thisForm = $(this);
                              resetLoginFormErrors();

                              $.post({
                                       url: contextPath +
                                            config.security.loginConnector,
                                       data: $thisForm.serialize(),
                                       statusCode: {
                                         200: function ()
                                         {
                                           refreshPage();
                                         },
                                         401: function ()
                                         {
                                           $thisForm.find("#login_fail").text(lg.INVALID_CREDENTIALS);
                                           $thisForm.addClass("has-error");
                                         }
                                       }
                                     });

                              return false;
                            });
  }

  /**
   * Obtain the path of the current folder.
   *
   * @returns {*|{}}
   */
  var getCurrentPath = function ()
  {
    return $('#currentpath').val() || '/';
  };


// Test if a given url exists
  function file_exists(url)
  {
    // http://kevin.vanzonneveld.net
    // +   original by: Enrique Gonzalez
    // +      input by: Jani Hartikainen
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // %        note 1: This function uses XmlHttpRequest and cannot retrieve
    // resource from different domain. %        note 1: Synchronous so may lock
    // up browser, mainly here for study purposes.  *     example 1:
    // file_exists('http://kevin.vanzonneveld.net/pj_test_supportfile_1.htm');
    // *     returns 1: '123'
    var req = this.window.ActiveXObject ?
              new ActiveXObject("Microsoft.XMLHTTP") : new XMLHttpRequest();
    if (!req)
    {
      throw new Error('XMLHttpRequest not supported');
    }

    // HEAD Results are usually shorter (faster) than GET
    req.open('HEAD', url, false);
    req.send(null);

    return req.status == 200;
  }

// preg_replace
// Code from : http://xuxu.fr/2006/05/20/preg-replace-javascript/
  var preg_replace = function (array_pattern, array_pattern_replace, str)
  {
    var new_str = String(str);
    for (i = 0; i < array_pattern.length; i++)
    {
      var reg_exp = new RegExp(array_pattern[i], "g");
      var val_to_replace = array_pattern_replace[i];
      new_str = new_str.replace(reg_exp, val_to_replace);
    }
    return new_str;
  };

  /**
   * Check if the given string is a valid item name.
   *
   * @param str
   * @returns {*|boolean}
   */
  var validFilename = function (str)
  {
    return str && /[a-zA-Z0-9_\-()=+!,;:@*$.]+/.test(str);
  };

// cleanString (), on the same model as server side (connector)
// cleanString
  var cleanString = function (str)
  {
    var p_search = ["Š", "š", "Đ", "đ", "Ž", "ž", "Č", "č", "Ć", "ć", "À",
                    "Á", "Â", "Ã", "Ä", "Å", "Æ", "Ç", "È", "É", "Ê", "Ë", "Ì",
                    "Í", "Î", "Ï",
                    "Ñ", "Ò", "Ó", "Ô", "Õ", "Ö", "Ő", "Ø", "Ù", "Ú", "Û", "Ü",
                    "Ý", "Þ", "ß",
                    "à", "á", "â", "ã", "ä", "å", "æ", "ç", "è", "é", "ê", "ë",
                    "ì", "í",
                    "î", "ï", "ð", "ñ", "ò", "ó", "ô", "õ", "ö", "ő", "ø", "ù",
                    "ú", "û", "ü",
                    "ý", "ý", "þ", "ÿ", "Ŕ", "ŕ", " ", "'", "/"
    ];
    var p_replace = ["S", "s", "Dj", "dj", "Z", "z", "C", "c", "C", "c", "A",
                     "A", "A", "A", "A", "A", "A", "C", "E", "E", "E", "E", "I",
                     "I", "I", "I",
                     "N", "O", "O", "O", "O", "O", "O", "O", "U", "U", "U", "U",
                     "Y", "B", "Ss",
                     "a", "a", "a", "a", "a", "a", "a", "c", "e", "e", "e", "e",
                     "i", "i",
                     "i", "i", "o", "n", "o", "o", "o", "o", "o", "o", "o", "u",
                     "u", "u", "u",
                     "y", "y", "b", "y", "R", "r", "_", "_", ""
    ];

    var cleaned = preg_replace(p_search, p_replace, str);

    // allow only latin alphabet
    if (config.options.chars_only_latin)
    {
      cleaned = cleaned.replace(/[^_a-zA-Z0-9]/g, "");
    }

    cleaned = cleaned.replace(/[_]+/g, "_");

    // prevent bug https://github.com/simogeo/Filemanager/issues/474
    if (cleaned == "")
    {
      cleaned = "unsupportedCharsReplacement";
    }

    return cleaned;
  };

// nameFormat (), separate filename from extension before calling cleanString()
// nameFormat
  var nameFormat = function (input)
  {
    var filename = '';
    if (input.lastIndexOf('.') != -1)
    {
      filename = cleanString(input.substr(0, input.lastIndexOf('.')));
      filename += '.' + input.split('.').pop();
    }
    else
    {
      filename = cleanString(input);
    }
    return filename;
  };

//Converts bytes to kb, mb, or gb as needed for display.
  var formatBytes = function (bytes)
  {
    var n = parseFloat(bytes);
    var d = parseFloat(1024);
    var c = 0;
    var u = [lg.bytes, lg.kb, lg.mb, lg.gb];

    while (true)
    {
      if (n < d)
      {
        n = Math.round(n * 100) / 100;
        return n + u[c];
      }
      else
      {
        n /= d;
        c += 1;
      }
    }
  };


// Test if Data structure has the 'cap' capability
// 'cap' is one of 'select', 'rename', 'delete', 'download', move
  function has_capability(data, cap)
  {
    if (data['File Type'] == 'dir' && cap == 'replace')
    {
      return false;
    }

    if (data['File Type'] == 'dir' && cap == 'download')
    {
      return config.security.allowFolderDownload == true;
    }

    if (typeof(data['Capabilities']) == "undefined")
    {
      return true;
    }
    else
    {
      return $.inArray(cap, data['Capabilities']) > -1;
    }
  }

// Test if file is authorized
  var isAuthorizedFile = function (filename)
  {
    var ext = getExtension(filename);

    // no extension is allowed
    if ((ext == '') && (config.security.allowNoExtension == true))
    {
      return true;
    }

    if (config.security.uploadPolicy == 'DISALLOW_ALL')
    {
      if ($.inArray(ext, config.security.uploadRestrictions) != -1)
      {
        return true;
      }
    }
    if (config.security.uploadPolicy == 'ALLOW_ALL')
    {
      if ($.inArray(ext, config.security.uploadRestrictions) == -1)
      {
        return true;
      }
    }

    return false;
  };

// return filename extension
  var getExtension = function (filename)
  {
    if (filename.split('.').length == 1)
    {
      return "";
    }
    return filename.split('.').pop().toLowerCase();
  };

// return filename without extension {
  var getFilename = function (filename)
  {
    if (filename.lastIndexOf('.') != -1)
    {
      return filename.substring(0, filename.lastIndexOf('.'));
    }
    else
    {
      return filename;
    }
  };

//Test if is editable file
  var isEditableFile = function (filename)
  {
    return $.inArray(getExtension(filename), config.edit.editExt) != -1;
  };

// Test if file is supported web video file
  var isVideoFile = function (filename)
  {
    return $.inArray(getExtension(filename), config.videos.videosExt) != -1;
  };

// Test if file is supported web audio file
  var isAudioFile = function (filename)
  {
    return $.inArray(getExtension(filename), config.audios.audiosExt) != -1;
  };

// Test if file is pdf file
  var isPdfFile = function (filename)
  {
    return $.inArray(getExtension(filename), config.pdfs.pdfsExt) != -1;
  };

  var postFormatPlayer = function (_playerHTML)
  {
    $fileInfo.find('img').remove();
    $fileInfo.find('#preview').find('#main-title').before(_playerHTML);
  };

// Return HTML video player
  var getVideoPlayer = function (data)
  {
    var code = '<video width=' + config.videos.videosPlayerWidth + ' height=' +
               config.videos.videosPlayerHeight + ' src="' + data['Path'] +
               '" controls="controls">';
    code += '<img src="' + data['Preview'] + '" />';
    code += '</video>';

    postFormatPlayer(code);
  };

//Return HTML audio player
  var getAudioPlayer = function (data)
  {
    var code = '<audio src="' + data['Path'] + '" controls="controls">';
    code += '<img src="' + data['Preview'] + '" />';
    code += '</audio>';

    postFormatPlayer(code);
  };

//Return PDF Reader
  var getPdfReader = function (data)
  {
    var code = '<iframe id="fm-pdf-viewer" src = "scripts/ViewerJS/index.html#' +
               data['Path'] + '" width="' + config.pdfs.pdfsReaderWidth +
               '" height="' + config.pdfs.pdfsReaderHeight +
               '" allowfullscreen webkitallowfullscreen></iframe>';

    postFormatPlayer(code);
  };

  /**
   * Prompt for a link name and URL, then issue a create request to the server.
   */
  var popupCreateLink = function ()
  {
    var linkURLExample = 'http://example.com/path';
    var msg = '<div class="form-group"><label for="link_name" class="hidden">' +
              lg.name
              +
              '</label><input id="link_name" name="link_name" type="text" class="form-control" placeholder="' +
              lg.name + '" /></div>'
              +
              '<div class="form-group"><label for="link_name" class="hidden">' +
              linkURLExample
              +
              '</label><input id="link_url" name="link_url" type="url" class="form-control" placeholder="' +
              linkURLExample + '" /></div>';

    var errorMessage;

    var doLinkCreate = function (_formVals)
    {
      var returnValue = null;
      var linkName = _formVals["link_name"];

      if (validFilename(linkName))
      {
        $.ajax(
          {
            url: contextPath + config.options.linkConnector
                 + getCurrentPath() +
                 "/" + encodeURIComponent(linkName),
            method: "PUT",
            data: JSON.stringify(_formVals),
            contentType: "application/json",
            statusCode: {
              201: function ()
              {
                returnValue = true;
              },
              400: function ()
              {
                errorMessage = lg.ERROR_SERVER;
                returnValue = false;
              },
              401: function ()
              {
                errorMessage = lg.NOT_ALLOWED_SYSTEM;
                returnValue = false;
              },
              403: function ()
              {
                errorMessage = lg.NOT_ALLOWED_SYSTEM;
                returnValue = false;
              },
              409: function ()
              {
                errorMessage = lg.LINK_ALREADY_EXISTS.replace(/%s/g, linkName);
                returnValue = false;
              }
            }
          });

        if (returnValue === null)
        {
          returnValue = true;
        }
      }
      else
      {
        errorMessage = lg.INVALID_ITEM_NAME;
        returnValue = false;
      }

      return returnValue;
    };

    $.prompt.disableStateButtons("link_creation");

    var btns = [];
    btns.push({
                "title": lg.create_external_link,
                "value": true,
                "classes": "btn btn-primary"
              });
    btns.push({
                "title": lg.cancel,
                "value": false,
                "classes": "btn btn-default"
              });

    $.prompt({
               input: {
                 focus: "#link_name",
                 buttons: btns,
                 html: msg,
                 submit: function (e, value, message, formVals)
                 {
                   if (value === true)
                   {
                     e.preventDefault();
                     $.prompt.nextState(function (event)
                                        {
                                          event.preventDefault();
                                          var nextState = doLinkCreate(formVals)
                                            ? "successful" : "unsuccessful";
                                          $.prompt.goToState(nextState);
                                          return false;
                                        });
                   }
                   else
                   {
                     return true;
                   }
                 }
               },
               link_creation: {
                 html: lg.creating
               },
               successful: {
                 html: lg.successful_added_link,
                 buttons: [{
                   "title": lg.close,
                   "value": false,
                   "classes": "btn btn-success"
                 }],
                 submit: refreshPage
               },
               unsuccessful: {
                 html: errorMessage ? errorMessage : lg.unsuccessful_added_link
               }
             });
  };

  var setSrcNodes = function ()
  {
    var selectedItems =
      $('tr.selected > td:nth-child(2) > span.glyphicon-pencil > a');
    var selectedPaths = [];

    $(selectedItems).each(function (index, item)
                          {
                            selectedPaths.push($(item).attr('path'));
                          });

    return (selectedPaths.length > 0) ? selectedPaths.join(",") : "";
  };

  var setLinker = function ()
  {
    $('#new_vospace_link').off().click(function ()
                                       {
                                         linkItem();
                                       });
  };

  var setMover = function ()
  {
    $('#move').off().click(function ()
                           {
                             var srcNodeList = setSrcNodes();
                             if (srcNodeList.length > 0)
                             {
                               moveItem(srcNodeList);
                             }
                           });
  };

// Sets the folder status, upload, and new folder functions
// to the path specified. Called on initial page load and
// whenever a new directory is selected.
  var setUploader = function ()
  {
    $("#new_external_link").off().click(function ()
                                        {
                                          popupCreateLink();
                                        });

    $('#uploadfolder').off().click(function ()
                                   {
                                     var form = document.createElement("form");
                                     form.setAttribute("method", "POST");
                                     form.setAttribute("action",
                                                       config.upload.url
                                                       + getCurrentPath());

                                     // Move the submit function to another
                                     // variable so that it doesn't get
                                     // overwritten.
                                     form._submit_function_ = form.submit;

                                     document.body.appendChild(form);

                                     form._submit_function_();

                                     return false;
                                   });

    $('#newfolder').off().click(function ()
                                {
                                  var buttonAction = function (event, value,
                                                               message,
                                                               formVals)
                                  {
                                    if (value === true)
                                    {
                                      var fname = formVals['fname'];

                                      if (validFilename(fname))
                                      {
                                        $.ajax(
                                          {
                                            url: contextPath +
                                                 config.options.folderConnector
                                                 + getCurrentPath() + "/"
                                                 + encodeURIComponent(fname),
                                            method: "PUT",
                                            contentType: "application/json",
                                            statusCode: {
                                              201: function ()
                                              {
                                                $.prompt(lg.successful_added_folder,
                                                         {
                                                           submit: refreshPage
                                                         });
                                              },
                                              400: function ()
                                              {

                                              },
                                              401: function ()
                                              {
                                                $.prompt(lg.NOT_ALLOWED_SYSTEM);
                                              },
                                              403: function ()
                                              {
                                                $.prompt(lg.NOT_ALLOWED_SYSTEM);
                                              },
                                              409: function ()
                                              {
                                                $.prompt(lg.DIRECTORY_ALREADY_EXISTS.replace(/%s/g, fname));
                                              }
                                            }
                                          });
                                      }
                                      else
                                      {
                                        $.prompt(lg.INVALID_ITEM_NAME);
                                      }
                                    }
                                    else
                                    {
                                      return true;
                                    }
                                  };

                                  var msg = '<input id="fname" name="fname" type="text" class="form-control" placeholder="' +
                                            lg.prompt_foldername +
                                            '" value="" />';

                                  var btns = [];
                                  btns.push({
                                              "title": lg.create_folder,
                                              "value": true,
                                              "classes": "btn btn-primary"
                                            });
                                  btns.push({
                                              "title": lg.cancel,
                                              "value": false,
                                              "classes": "btn btn-default"
                                            });
                                  $.prompt(msg,
                                           {
                                             submit: buttonAction,
                                             focus: "#fname",
                                             buttons: btns
                                           });
                                });
  };

// Binds specific actions to the toolbar in detail views.
// Called when detail views are loaded.
  var bindToolbar = function (data)
  {
    // Cosmetic
    $fileInfo.find("button").each(function ()
                                  {
                                    // check if span doesn't exist yet, when
                                    // bindToolbar called from renameItem for
                                    // example
                                    if ($(this).find('span').length == 0)
                                    {
                                      $(this).wrapInner('<span></span>');
                                    }
                                  });

    if (!has_capability(data, 'select'))
    {
      $fileInfo.find('button#select').hide();
    }
    else
    {
      $fileInfo.find('button#select').click(function ()
                                            {
                                              selectItem(data);
                                            }).show();

      if (window.opener || window.tinyMCEPopup)
      {
        var $previewImage = $("#preview").find("img");
        $previewImage.attr('title', lg.select);
        $previewImage.click(function ()
                            {
                              selectItem(data);
                            }).css("cursor", "pointer");
      }
    }

    if (!has_capability(data, 'rename'))
    {
      $fileInfo.find('button#rename').hide();
    }
    else
    {
      $fileInfo.find('button#rename').click(function ()
                                            {
                                              var newName = renameItem(data);
                                              if (newName.length)
                                              {
                                                $('#fileinfo > h1').text(newName);
                                              }
                                            }).show();
    }

    // @todo
    if (!has_capability(data, 'replace'))
    {
      $fileInfo.find('button#replace').hide();
    }
    else
    {
      $fileInfo.find('button#replace').click(function ()
                                             {
                                               replaceItem(data);
                                             }).show();
    }
  };


  /*---------------------------------------------------------
   Functions for Editing Folder Permissions
   ---------------------------------------------------------*/
  // Highlighting the input box if an invalid selection is entered
  var togglePromptError = function (fieldId, msgFieldId, msg, setOn)
  {
    if (setOn === "on")
    {
      $(fieldId).addClass("has-error");
      $(msgFieldId).text(msg);
    }
    else
    {
      $(fieldId).removeClass("has-error");
      $(msgFieldId).text(msg);
    }
  };


  // Store contents of /storage/groups call so it can be
  // rendered in the read and write group dropdowns
  var autoCompleteList = [];

  // Callback from ajax call to /storage/groups
  var handleLoadAutocomplete = function (autocompleteData)
  {
    autoCompleteList = autocompleteData;
    $("#readGroup").autocomplete(
      {
        appendTo: '#readGroupDiv',
        source: function (request, response)
        {
          // Reduce results to 10 for display
          var results = $.ui.autocomplete.filter(autocompleteData, request.term);
          response(results.slice(0, 10));
        },
        minLength: 2
      });

    $("#readGroup").keyup(function ()
                          {
                            togglePromptError("#readGroupDiv", "#readGroupLabel", lg.READ_GROUP, "off");
                          });

    $("#writeGroup").autocomplete(
      {
        appendTo: '#writeGroupDiv',
        source: function (request, response)
        {
          // Reduce results to 10 for display
          var results = $.ui.autocomplete.filter(autocompleteData, request.term);
          response(results.slice(0, 10));
        },
        minLength: 2
      });

    $("#writeGroup").keyup(function ()
                           {
                             togglePromptError("#writeGroupDiv", "#writeGroupLabel", lg.WRITE_GROUP, "off");
                           });

  };


  var isPermissionChanged = function (formValues)
  {
    // Values to check against are in the currently edited icon
    var clickedEditIcon = $('.editing')[0];
    var isChanged = false;

    if (formValues["writeGroup"] !== clickedEditIcon.getAttribute("writeGroup"))
    {
      return true;
    }

    if (formValues["readGroup"] !== clickedEditIcon.getAttribute("readGroup"))
    {
      return true;
    }

    if (formValues["recursive"] === "on")
    {
      return true;
    }

    if ((clickedEditIcon.getAttribute("readable") === "true" &&
         formValues["publicPermission"] === "on") ||
        (clickedEditIcon.getAttribute("readable") === "false") &&
        (typeof(formValues["publicPermission"]) === "undefined"))
    {
      return false;
    }
    else
    {
      return true;
    }

    return false;

  }

  // Submit function for $.prompt instance
  var handleEditPermissions = function (event, value, message, formVals)
  {
    var returnValue = false;
    var doneEditing = true;

    if (value === true)
    {
      // Form has not been modified, leave it open
      if ($(".listener-hook").hasClass("disabled"))
      {
        return false;
      }

      if (isPermissionChanged(formVals) === true)
      {
        var readGroupValid = false;
        var writeGroupValid = false;

        // validate that form has changed or not. If not, don't submit.

        if ((typeof(formVals["readGroup"]) === "undefined") ||
            (formVals["readGroup"] === "") ||
            ($.inArray(formVals["readGroup"], autoCompleteList) >= 0))
        {
          readGroupValid = true;
        }

        if ((typeof(formVals["writeGroup"]) === "undefined") ||
            (formVals["writeGroup"] === "") ||
            ($.inArray(formVals["writeGroup"], autoCompleteList) >= 0))
        {
          writeGroupValid = true;
        }

        // If box not checked, does not get added to formVals
        if (typeof(formVals["publicPermission"]) === "undefined")
        {
          formVals["publicPermission"] = "off";
        }

        if ((writeGroupValid === true) && (readGroupValid === true))
        {
          var itemPath = formVals['itemPath'];

          var url = contextPath + config.options.itemConnector + itemPath;

          var dataStr = JSON.stringify(formVals);
          $.ajax(
            {
              url: url,
              method: "POST",
              contentType: "application/json",
              data: dataStr,
              statusCode: {
                202: function ()
                {
                  $.prompt(lg.permissions_recursive_submitted,
                           {
                             submit: refreshPage
                           });
                },
                204: function ()
                {
                  $.prompt(lg.permissions_modified,
                           {
                             submit: refreshPage
                           });
                },
                400: function ()
                {
                  $.prompt(lg.NOT_ALLOWED_SYSTEM);
                },
                401: function ()
                {
                  $.prompt(lg.NOT_ALLOWED_SYSTEM);
                },
                403: function ()
                {
                  $.prompt(lg.NOT_ALLOWED_SYSTEM);
                },
                409: function ()
                {
                  $.prompt(lg.NOT_ALLOWED_SYSTEM.replace(/%s/g, fname));
                },
                500: function ()
                {
                  $.prompt(lg.server_error);
                }
              }
            });

        }
        else
        {
          // Mark fields that are in error, stay on form
          event.preventDefault();
          if (readGroupValid === false)
          {
            togglePromptError("#readGroupDiv", "#readGroupLabel", lg.READ_GROUP, "on");
          }
          if (writeGroupValid === false)
          {
            togglePromptError("#writeGroupDiv", "#writeGroupLabel", lg.WRITE_GROUP, "on");
          }

          doneEditing = false;
        }
      }
      else
      {
        $.prompt(lg.permissions_not_modified);
      }  // end if isPermissionChanged block

    }
    else
    {
      returnValue = true;
    }

    if (doneEditing === true)
    {
      // Remove flag showing this is active edit icon
      $('.editing')[0].setAttribute("class", "");
    }

    if (returnValue === true)
    {
      return true;
    }

  }; // end handleEditPermissions


  $(document).on('click', '.glyphicon-pencil', function (event)
  {
    // Pull attributes from edit icon
    var iconAnchor = $(event.currentTarget).find("a")[0];

    // Flag this icon as the currently active one
    // will be referenced for seeing if form values have changed
    iconAnchor.setAttribute("class", "editing");

    var checkboxState = "";
    var readGroupBoxDisabled = "false";
    if (iconAnchor.getAttribute("readable") === "true")
    {
      checkboxState = "checked";
    }

    var msg =
      '<div class="form-group fm-prompt">' +
      '<label for="publicPermission" class="control-label col-sm-4">' +
      lg.public_question + '</label>' +
      '<div class="col-sm-7">' +
      '<input style="margin: 9px 0 0;" class="action-hook" type="checkbox" id="publicPermission" name="publicPermission" ' +
      checkboxState + '>' +
      '</div>' +
      '</div>' +
      '<div class="form-group ui-front fm-prompt" id="readGroupDiv"> ' +
      '<label for="readGroup" id="readGroupLabel" class="control-label col-sm-4">' +
      lg.READ_GROUP + '</label>' +
      '<div id="readGroupInputDiv" class="col-sm-7"> ' +
      '<input type="text" class="form-control  ui-autocomplete-input action-hook" id="readGroup" name="readGroup" placeholder="' +
      lg.group_name_program_id + '">' +
      '</div>' +
      '</div>' +
      '<div class="form-group ui-front fm-prompt" id="writeGroupDiv">' +
      '<label for="writeGroup" id="writeGroupLabel" class="control-label col-sm-4">' +
      lg.WRITE_GROUP + '</label>' +
      '<div class="col-sm-7">' +
      '<input type="text" class="form-control action-hook" id="writeGroup" name="writeGroup" placeholder="' +
      lg.group_name_program_id + '">' +
      '</div>' +
      '</div>' +
      '<div class="form-group fm-prompt">' +
      '<label for="recursive" class="control-label col-sm-4"">' + lg.recursive +
      '</label>' +
      '<div class="col-sm-7">' +
      '<input style="margin: 9px 0 0;" class="action-hook" type="checkbox" id="recursive" name="recursive">' +
      '</div>' +
      '</div>' +
      '<div class="form-group fm-prompt">' +
      '<div class="col-sm-4" >' +
      '</div>' +
      '<div class="col-sm-7 prompt-link">' +
      '<a href="http://www.canfar.phys.uvic.ca/canfar/groups" target="_blank">Manage Groups</a>' +
      '<input type="text" class="hidden" name="itemPath" id="itemPath" value="' +
      iconAnchor.getAttribute("path") + '">' +
      '</div>' +
      '</div>';


    var btns = [];
    btns.push
    ({
       "name": "b1",
       "title": lg.save,
       "value": true,
       "classes": "btn btn-primary listener-hook"
     });
    btns.push
    ({
       "name": "b2",
       "title": lg.cancel,
       "value": false,
       "classes": "btn btn-default"
     });

    // 'classes' entry below is to enable bootstrap to
    // handle styling, including form-horizontal

    var states = {
      state0: {
        title: '<h3 class="prompt-h3">' + iconAnchor.getAttribute("itemName") +
               '</h3>',
        html: msg,
        buttons: btns,
        submit: handleEditPermissions
      }
    };
    $.prompt(states, {
      classes: {
        form: 'form-horizontal',
        box: '',
        fade: '',
        prompt: '',
        close: '',
        title: 'lead',
        message: '',
        buttons: '',
        button: 'btn',
        defaultButton: 'btn-primary'
      },
      loaded: function (event)
      {
        // Get the group names list for populating the dropdown first
        $.ajax(
          {
            type: 'GET',
            url: contextPath + "groups",
            success: function (returnValue)
            {
              handleLoadAutocomplete(returnValue);
            },
            error: function (errorValue)
            {
              $.prompt(lg.ERROR_GROUPNAMES);
            }
          }
        );

        // Set initial form state
        $("#readGroup").val(iconAnchor.getAttribute("readGroup"));
        $("#writeGroup").val(iconAnchor.getAttribute("writeGroup"));
        var listenerHook = $(".listener-hook");
        listenerHook.addClass("disabled");

        $(".action-hook").on('click', function (event)
        {
          listenerHook.removeClass("disabled");
        });

      }
    }); // end prompt declaration

  });


  /*---------------------------------------------------------
   Item Actions
   ---------------------------------------------------------*/

// Calls the SetUrl function for FCKEditor compatibility,
// passes file path, dimensions, and alt text back to the
// opening window. Triggered by clicking the "Select"
// button in detail views or choosing the "Select"
// contextual menu option in list views.
// NOTE: closes the window when finished.
  var selectItem = function (data)
  {
    var url;

    if (config.options.baseUrl !== false)
    {
      url = smartPath(baseUrl, data['Path'].replace(fileRoot, ""));
    }
    else
    {
      url = data['Path'];
    }

    if (window.opener || window.tinyMCEPopup || $.urlParam('field_name') ||
        $.urlParam('CKEditorCleanUpFuncNum') || $.urlParam('CKEditor'))
    {
      if (window.tinyMCEPopup)
      {
        // use TinyMCE > 3.0 integration method
        var win = tinyMCEPopup.getWindowArg("window");
        win.document.getElementById(tinyMCEPopup.getWindowArg("input")).value =
          url;
        if (typeof(win.ImageDialog) != "undefined")
        {
          // Update image dimensions
          if (win.ImageDialog.getImageData)
          {
            win.ImageDialog.getImageData();
          }

          // Preview if necessary
          if (win.ImageDialog.showPreviewImage)
          {
            win.ImageDialog.showPreviewImage(url);
          }
        }
        tinyMCEPopup.close();
      }
      else
      {
        // tinymce 4 and colorbox
        if ($.urlParam('field_name'))
        {
          parent.document.getElementById($.urlParam('field_name')).value = url;

          if (typeof parent.tinyMCE !== "undefined")
          {
            parent.tinyMCE.activeEditor.windowManager.close();
          }
          if (typeof parent.$.fn.colorbox !== "undefined")
          {
            parent.$.fn.colorbox.close();
          }
        }

        else if ($.urlParam('CKEditor'))
        {
          // use CKEditor 3.0 + integration method
          if (window.opener)
          {
            // Popup
            window.opener.CKEDITOR.tools.callFunction($.urlParam('CKEditorFuncNum'), url);
          }
          else
          {
            // Modal (in iframe)
            parent.CKEDITOR.tools.callFunction($.urlParam('CKEditorFuncNum'), url);
            parent.CKEDITOR.tools.callFunction($.urlParam('CKEditorCleanUpFuncNum'));
          }
        }
        else
        {
          // use FCKEditor 2.0 integration method
          if (data['Properties']['Width'] != '')
          {
            var p = url;
            var w = data['Properties']['Width'];
            var h = data['Properties']['Height'];
            window.opener.SetUrl(p, w, h);
          }
          else
          {
            window.opener.SetUrl(url);
          }
        }

        if (window.opener)
        {
          window.close();
        }
      }
    }
    else
    {
      $.prompt(lg.fck_select_integration);
    }
  };

// Renames the current item and returns the new name.
// Called by clicking the "Rename" button in detail views
// or choosing the "Rename" contextual menu option in
// list views.
  var renameItem = function (data)
  {
    var finalName = '';
    var fileName = config.security.allowChangeExtensions ? data['Filename'] :
                   getFilename(data['Filename']);
    var msg = lg.new_filename +
              ' : <input id="rname" name="rname" type="text" value="' +
              fileName + '" />';

    var getNewName = function (v, m)
    {
      if (v != 1)
      {
        return false;
      }
      rname = m.children('#rname').val();

      if (rname != '')
      {

        var givenName = rname;

        if (!config.security.allowChangeExtensions)
        {
          givenName = nameFormat(rname);
          var suffix = getExtension(data['Filename']);
          if (suffix.length > 0)
          {
            givenName = givenName + '.' + suffix;
          }
        }

        // File only - Check if file extension is allowed
        if (data['Path'].charAt(data['Path'].length - 1) != '/' &&
            !isAuthorizedFile(givenName))
        {
          var str = '<p>' + lg.INVALID_FILE_TYPE + '</p>';
          if (config.security.uploadPolicy == 'DISALLOW_ALL')
          {
            str += '<p>' + lg.ALLOWED_FILE_TYPE +
                   config.security.uploadRestrictions.join(', ') + '.</p>';
          }
          if (config.security.uploadPolicy == 'ALLOW_ALL')
          {
            str += '<p>' + lg.DISALLOWED_FILE_TYPE +
                   config.security.uploadRestrictions.join(', ') + '.</p>';
          }
          $("#filepath").val('');
          $.prompt(str);
          return false;
        }

        var connectString = fileConnector + '?mode=rename&old=' +
                            encodeURIComponent(data['Path']) + '&new=' +
                            encodeURIComponent(givenName) + '&config=' +
                            userconfig;

        $.ajax({
                 type: 'GET',
                 url: connectString,
                 dataType: 'json',
                 async: false,
                 success: function (result)
                 {
                   if (result['Code'] == 0)
                   {
                     var newPath = result['New Path'];
                     var newName = result['New Name'];
                     var oldPath = result['Old Path'];

                     var $previewH1 = $("#preview").find("h1");

                     var title = $previewH1.attr("title");

                     if (typeof title != "undefined" && title == oldPath)
                     {
                       $previewH1.text(newName);
                     }

                     var $tdInfo =
                       $fileInfo.find('td[data-path="' + oldPath + '"]');
                     $tdInfo.text(newName).attr('data-path', newPath);

                     $previewH1.html(newName);

                     // actualized data for binding
                     data['Path'] = newPath;
                     data['Filename'] = newName;

                     // Bind toolbar functions.
                     $fileInfo.find('button#rename, #delete, button#download').off();
                     bindToolbar(data);

                     if (config.options.showConfirmation)
                     {
                       $.prompt(lg.successful_rename);
                     }
                   }
                   else
                   {
                     $.prompt(result['Error']);
                   }

                   finalName = result['New Name'];
                 }
               });
      }
    };
    var btns = {};
    btns[lg.rename] = true;
    btns[lg.cancel] = false;
    $.prompt(msg, {
      callback: getNewName,
      buttons: btns
    });

    return finalName;
  };

// Replace the current file and keep the same name.
// Called by clicking the "Replace" button in detail views
// or choosing the "Replace" contextual menu option in
// list views.
  var replaceItem = function (data)
  {
    // we auto-submit form when user filled it up
    $('#fileR').on('change', function ()
    {
      $(this).closest("form#toolbar").submit();
    });

    var $toolbar = $("#toolbar");

    // we set the connector to send data to
    $toolbar.attr('action', fileConnector).attr('method', 'post');

    // submission script
    $toolbar.ajaxForm({
                        target: '#uploadresponse',
                        beforeSubmit: function (arr, form, options)
                        {

                          var newFile = $('#fileR', form).val();

                          // Test if a value is given
                          if (newFile == '')
                          {
                            return false;
                          }

                          // Check if file extension is matching with the
                          // original
                          if (getExtension(newFile) != data["File Type"])
                          {
                            $.prompt(lg.ERROR_REPLACING_FILE + " ." +
                                     getExtension(data["Filename"]));
                            return false;
                          }
                          $('#replace').attr('disabled', true);
                          $('#upload span').addClass('loading').text(lg.loading_data);

                          // if config.upload.itimit == auto we
                          // delegate size test to connector
                          if (typeof FileReader !== "undefined" &&
                              typeof config.upload.fileSizeLimit != "auto")
                          {
                            // Check file size using html5 FileReader API
                            var size = $('#fileR', form).get(0).files[0].size;
                            if (size >
                                config.upload.fileSizeLimit * 1024 * 1024)
                            {
                              $.prompt("<p>" + lg.file_too_big +
                                       "</p><p>" + lg.file_size_limit +
                                       config.upload.fileSizeLimit + " " +
                                       lg.mb + ".</p>");
                              $('#upload').removeAttr('disabled').find("span").removeClass('loading').text(lg.upload);
                              return false;
                            }
                          }
                        },
                        error: function (jqXHR, textStatus, errorThrown)
                        {
                          $('#upload').removeAttr('disabled').find("span").removeClass('loading').text(lg.upload);
                          $.prompt(lg.ERROR_UPLOADING_FILE);
                        },
                        success: function (result)
                        {
                          var data = jQuery.parseJSON($('#uploadresponse').find('textarea').text());

                          if (data['Code'] == 0)
                          {
                            var fullpath = data["Path"] + '/' +
                                           data["Name"];

                            // Reloading file info
                            getFileInfo(fullpath);

                            // Visual effects for user to see action is
                            // successful
                            $('#preview').find('img').hide().fadeIn('slow'); // on
                                                                             // right
                                                                             // panel

                            if (config.options.showConfirmation)
                            {
                              $.prompt(lg.successful_replace);
                            }

                          }
                          else
                          {
                            $.prompt(data['Error']);
                          }
                          $('#replace').removeAttr('disabled');
                          $('#upload span').removeClass('loading').text(lg.upload);
                        }
                      });

    // we pass data path value - original file
    $('#newfilepath').val(data["Path"]);

    // we open the input file dialog window
    $('#fileR').click();
  };

  function ItemLayer()
  {
    ItemLayer.makeNode = function ()
    {
      return {name: null, path: null, uri: null, child: null};
    };

    this.head = new Array();

    this.addNode = function (name, path, uri)
    {
      var node = ItemLayer.makeNode();
      node.name = name;
      node.path = path;
      node.uri = uri;
      this.head.push(node);
    }

    this.findNodeOnCurrentLayer = function (name)
    {
      for (var i = 0; i < this.head.length; i++)
      {
        if (this.head[i].name == name)
        {
          return this.head[i];
        }
      }

      return null;
    }

    this.findNode = function (fullName)
    {
      var node = null;
      var tempName = '/';

      if (stringUtil.hasText(fullName))
      {
        var name = fullName.split('/')[1];
        node = this.findNodeOnCurrentLayer(name);
        tempName = tempName + name;
        if (fullName.length > tempName.length)
        {
          node = node.child.findNode(fullName.substr(tempName.length));
        }
      }

      return node;
    }

    this.toHTML = function ()
    {
      var returnHTML = '';

      $.each(this.head, function (index, node)
      {
        if (node.child == null)
        {
          returnHTML = returnHTML
                       + '<li><div class="layerItemName" fullName="' +
                       node.path + '" uri="' + node.uri + '">' + node.name
                       + '</div><ul></ul></li>';
        }
        else
        {
          returnHTML = returnHTML
                       + '<li><div class="layerItemName" fullName="' +
                       node.path + '" uri="' + node.uri + '">' + node.name
                       + '</div>';
          returnHTML = returnHTML + '<ul>' + node.child.toHTML() + '</ul>';
          returnHTML = returnHTML + '</li>';
        }
      });

      return returnHTML;
    }
  }

  function ItemTree()
  {
    this.root = null;

    this.findNode = function (name)
    {
      return this.root == null ? null : this.root.findNode(name);
    }

    this.addLayer = function (name, layer)
    {
      // name of root node is an empty string
      if (this.root == null || (!stringUtil.hasText(name)))
      {
        this.root = layer;
      }
      else
      {
        this.findNode(name).child = layer;
      }
    }

    this.toHTML = function ()
    {
      return returnHTML =
        '<ul class="collapsibleList">' + this.root.toHTML() + '</ul>';
    }
  }

  // When we get a page of cvs data from the database containing 
  // both files and folders, apply this filter to filter out all
  // files.
  var filterOutFiles = function (itemLayer, rowData)
  {
    if (rowData[8].includes('glyphicon-folder'))
    {
      itemLayer.addNode(rowData[1], rowData[9], rowData[10]);
    }
  }

  var filterOutNothing = function (itemLayer, rowData)
  {
    itemLayer.addNode(rowData[1], rowData[9], rowData[10]);
  }

// Link the current item to specified dir and returns the new name.
// Called by clicking the "VOSpace Link" menu item.
  var linkItem = function ()
  {
    var params =
      {
        bCancelName: "bCancelLinkto",
        bCancelTitle: lg.cancel,
        bOpName: "bLinkto",
        bOpTitle: lg.link,
        filter: filterOutNothing,
        opSuccess: lg.successful_linked,
        promptMsg: lg.please_select_link,
        submitFunction: doLink
      };

    processItem(params, "")
  }; // end linkItem

// Move the current item to specified dir and returns the new name.
// Called by clicking the "Move" button.
  var moveItem = function (srcNodeList)
  {
    var params =
      {
        bCancelName: "bCancelMoveto",
        bCancelTitle: lg.cancel,
        bOpName: "bMoveto",
        bOpTitle: lg.move,
        filter: filterOutFiles,
        opSuccess: lg.successful_moved,
        promptMsg: lg.please_select_folder,
        submitFunction: doMove,

      };

    processItem(params, srcNodeList)
  }; // end moveItem


  // Submit function for move usage of collapsible list prompt
  var doMove = function (event, value, msg, formVals)
  {
    if (value == true)
    {

      // Target destination for the operation
      var itemPath = formVals['destNode'];

      var url = contextPath + config.options.folderConnector + itemPath;

      var dataStr = JSON.stringify(formVals);
      $.ajax(
        {
          url: url,
          method: "POST",
          contentType: "application/json",
          data: dataStr,
          statusCode: {
            204: function ()
            {
              $.prompt(lg.successful_moved,
                       {
                         submit: refreshPage
                       });
            },
            401: function ()
            {
              $.prompt(lg.NOT_ALLOWED_SYSTEM);
            },
            403: function ()
            {
              $.prompt(lg.authorization_required);
            },
            409: function ()
            {
              $.prompt(lg.FILE_ALREADY_EXISTS.replace(/%s/g, formVals['destNode']));
            },
            500: function ()
            {
              $.prompt(lg.ERROR_SERVER);
            }
          }
        });
    } //end if value == true
  }; // end doMove

  // Submit function for link use of collapsible list prompt
  var doLink = function (event, value, msg, formVals)
  {
    if (value == true)
    {
      // for link
      var url = contextPath + config.options.linkConnector +
                $('#currentpath').val() + "/" + formVals['itemName'];

      formVals['link_url'] = formVals['selectedNodeURI'];

      var dataStr = JSON.stringify(formVals);
      $.ajax(
        {
          url: url,
          method: "PUT",
          contentType: "application/json",
          data: dataStr,
          statusCode: {
            201: function ()
            {
              $.prompt(lg.successful_linked,
                       {
                         submit: refreshPage
                       });
            },
            401: function ()
            {
              $.prompt(lg.NOT_ALLOWED_SYSTEM);
            },
            403: function ()
            {
              $.prompt(lg.authorization_required);
            },
            409: function ()
            {
              $.prompt(lg.LINK_ALREADY_EXISTS.replace(/%s/g, formVals['itemName']));
            },
            500: function ()
            {
              $.prompt(lg.ERROR_SERVER);
            }
          }
        });
    } //end if value == true
  }; // end doLink


// Link or move the current item to specified dir and returns the new name.
// Called by clicking the "VOSpace Link" menu item or the "Move" button.
  var processItem = function (params, srcNodeList)
  {
    var srcNodes = '';
    var fullName = '';
    var itemTree = new ItemTree();
    var itemLayer;
    var pageUrl = contextPath + config.options.pageConnector;
    var itemRequest = {};
    itemRequest.pageSize = defaultPageSize;
    var spinningWheel = $.parseHTML('<span id="itemsLoading" class="glyphicon glyphicon-refresh fast-right-spinner"></span>');

    $(document).on('.layerItemName').click(function (event)
                                           {
                                             var target = $(event)[0].target;
                                             var nameWithPath = $(target).attr('fullName');
                                             var node = itemTree.findNode(nameWithPath);
                                             // Clear all highlights
                                             $(".layerItemName.bg-success").removeClass("bg-success");

                                             $(target).addClass("bg-success");

                                             if (node != null &&
                                                 target.parentNode.className !=
                                                 'collapsibleListClosed')
                                             {
                                               if (node.child == null)
                                               {
                                                 // we have no sub-folders for
                                                 // this node, get them
                                                 fullName = nameWithPath;
                                                 pageUrl = contextPath +
                                                           config.options.pageConnector +
                                                           node.path;
                                                 itemRequest.startURI = null;
                                                 buildItemLayer(itemRequest, updateItemTree);
                                               }

                                               // display path should be the
                                               // current item, not entire path
                                               $('#destNodeDisplay').val(" " +
                                                                         node.name);

                                               // entire path (or should this
                                               // be URI?) is passed in to back
                                               // end
                                               $('#destNode').val(node.path);
                                               $('#selectedNodeURI').val(node.uri);
                                               $('#itemName').val(node.name);
                                               $(".listener-hook").removeClass("disabled");
                                             }
                                           });

    var getPageOfItems = function (_pageRequest, _callback)
    {
      if (!$('#itemsLoading').length)
      {
        $('.spinnerSpan').append(spinningWheel);
      }

      $.get({
              url: pageUrl,
              dataType: "text",
              data: _pageRequest
            })
        .done(function (csvData)
              {
                _callback(csvData);
              });
    };

    var updateComplete = function ()
    {
      // add the new layer to the tree and draw the item tree
      itemTree.addLayer(fullName, itemLayer);

      if ($('.collapsibleList').length)
      {
        // we already have the collapsible list, add to it
        var layerNodes = $.parseHTML(itemLayer.toHTML());
        var selectedNode = $('ul.collapsibleList').find('div[fullName="' +
                                                        fullName +
                                                        '"]').closest('li');
        var containerNode = selectedNode.find('ul');

        if (layerNodes == null)
        {
          // leaf node, indicate it in the collapsible list
          containerNode.remove();
          containerNode = selectedNode;
          containerNode[0].classList.remove('collapsibleListOpen');
        }
        else
        {
          // non-leaf node, insert as a list of sub-nodes
          containerNode.append(layerNodes);
        }

        CollapsibleLists.applyTo(containerNode[0], false);
      }
      else
      {
        // initial load, no collapsible list, draw the vospace root item layer
        var node = $.parseHTML(itemTree.toHTML())[0];
        CollapsibleLists.applyTo(node, false);
        $('.itemTree').append(node);
      }

      if ($('#itemsLoading').length)
      {
        $('#itemsLoading').remove();
      }
    };

    // callback to update the cached item tree
    var updateItemTree = function (cvsData)
    {
      var data = $.csv.toArrays(cvsData);
      var dl = data.length;

      if (dl > 0)
      {
        var startURI = '';
        for (var di = 0; di < dl; di++)
        {
          var rowData = data[di];
          // apply filter
          params['filter'](itemLayer, rowData);
          startURI = rowData[10];
        }

        itemRequest.startURI = startURI;
        if (dl == itemRequest.pageSize)
        {
          // current page is full, get the next page
          getPageOfItems(itemRequest, updateItemTree);
        }
        else
        {
          // last page is a partial page
          updateComplete();
        }
      }
      else
      {
        // last page is empty
        updateComplete();
      }
    };

    var buildItemLayer = function (_itemRequest, _callback)
    {
      itemLayer = new ItemLayer();
      getPageOfItems(_itemRequest, _callback);
    };


    // name of root node is an empty string
    buildItemLayer(itemRequest, updateItemTree);

    var msg = '<div class="mMoveto">' + params['promptMsg'] +
              '<span class="spinnerSpan"></span></div> ' +
              '<div id="itemTree" class="itemTree col-sm-12"></div>' +
              '<div class="form-group ui-front" id="destNodeDiv">' +
              '<label for="destNodeDisplay" id=destNodeDisplayLabel" class="control-label col-sm-3">' +
              lg.destination + '</label>' +
              '<div class="col-sm-9">' +
              '<input type="text" disabled="disabled" class="form-control" id="destNodeDisplay" name="destNodeDisplay" placeholder="' +
              lg.select + " " + lg.destination_folder + '">' +
              '</div>' +
              '</div>' +
              '<input type="text" class="hidden" name="destNode" id="destNode">' +
              '<input type="text" class="hidden" name="selectedNodeURI" id="selectedNodeURI">' +
              '<input type="text" class="hidden" name="itemName" id="itemName">' +
              '<input type="text" class="hidden" name="srcNodes" id="srcNodes" value="' +
              srcNodeList + '">';

    var btns = [];
    btns.push
    ({
       "name": params['bOpName'],
       "title": params['bOpTitle'],
       "value": true,
       "classes": "btn btn-primary listener-hook"
     });
    btns.push
    ({
       "name": params['bCancelName'],
       "title": params['bCancelTitle'],
       "value": false,
       "classes": "btn btn-default"
     });

    $.prompt(msg, {
      classes: {
        form: 'form-horizontal',
        box: '',
        fade: '',
        prompt: '',
        close: '',
        title: 'lead',
        message: '',
        buttons: '',
        button: 'btn',
        defaultButton: 'btn-primary'
      },
      loaded: function ()
      {
        $('.spinnerSpan').append(spinningWheel);
        $(".listener-hook").addClass("disabled");
      },
      submit: params['submitFunction'],
      buttons: btns
    });
  }; // end processItem

  $(document).on("click", "#delete",
                 function ()
                 {
                   var paths = [];

                   $.each($dt.rows({selected: true}).data(),
                          function (key, itemData)
                          {
                            paths.push(itemData[9]);
                          });

                   deleteItems(paths);
                 });

  /**
   * Prompts for confirmation, then deletes the items.
   * Called by clicking the "Delete" button after selecting items.
   *
   * @param paths     Array of paths to delete.
   * @returns {boolean}
   */
  var deleteItems = function (paths)
  {
    var isDeleted = false;
    var msg = lg.confirmation_delete;
    var deleteCount = paths.length;
    var successful = [];
    var unsuccessful = {};
    var totalCompleteCount = 0;

    var doDelete = function ()
    {
      for (var p = 0; p < deleteCount; p++)
      {
        var path = paths[p];

        $.ajax({
                 type: 'DELETE',
                 url: contextPath + config.options.itemConnector + path,
                 async: false,
                 statusCode: {
                   200: function ()
                   {
                     successful.push(path);
                   },
                   401: function ()
                   {
                     unsuccessful[path] = lg.ERROR_WRITING_PERM;
                   },
                   403: function ()
                   {
                     unsuccessful[path] = lg.ERROR_WRITING_PERM;
                   },
                   404: function ()
                   {
                     unsuccessful[path] = lg.ERROR_NO_SUCH_ITEM;
                   },
                   500: function ()
                   {
                     unsuccessful[path] = lg.ERROR_SERVER;
                   }
                 }
               })
          .always(function ()
                  {
                    totalCompleteCount++;
                  });
      }

      while (totalCompleteCount < deleteCount)
      {
        // Wait
      }

      return ($.isEmptyObject(unsuccessful));
    };

    var btns = [];
    btns.push({
                "title": lg.yes,
                "value": true,
                "classes": "btn btn-danger"
              });
    btns.push({
                "title": lg.no,
                "value": false,
                "classes": "btn btn-default"
              });

    if (deleteCount > 0)
    {
      $.prompt.disableStateButtons("deletion");

      $.prompt({
                 confirmation: {
                   html: msg,
                   buttons: btns,
                   submit: function (e, v, m, f)
                   {
                     if (v === true)
                     {
                       e.preventDefault();
                       $.prompt.nextState(function (event)
                                          {
                                            event.preventDefault();
                                            var nextState = doDelete(event)
                                              ? "successful" : "unsuccessful";
                                            $.prompt.goToState(nextState);
                                            return false;
                                          });
                       return false;
                     }
                     else
                     {
                       $.prompt.close();
                     }
                   }
                 },
                 deletion: {
                   html: lg.deleting_message.replace('%d', deleteCount)
                 },
                 successful: {
                   html: lg.successful_delete,
                   buttons: [{
                     "title": lg.close,
                     "value": false,
                     "classes": "btn btn-success"
                   }],
                   submit: refreshPage
                 },
                 unsuccessful: {
                   html: function ()
                   {
                     var output = "";

                     $.each(unsuccessful, function (path, error)
                     {
                       output += path + ": " + error + "<br />";
                     });

                     return lg.unsuccessful_delete + "<br />" + output;
                   }
                 }
               });
    }

    return isDeleted;
  };

  /**
   * Prompts for confirmation, then deletes the items.
   * Called by clicking the "Delete" button after selecting items.
   *
   * @param paths     Array of paths to delete.
   * @returns {boolean}
   */
  var downloadItems = function (paths)
  {
    var isDeleted = false;

    var deleteCount = paths.length;
    var successful = [];
    var unsuccessful = {};
    var totalCompleteCount = 0;

    var doDelete = function ()
    {
      for (var p = 0; p < deleteCount; p++)
      {
        var path = paths[p];

        $.ajax({
                 type: 'DELETE',
                 url: contextPath + config.options.itemConnector + path,
                 async: false,
                 statusCode: {
                   200: function ()
                   {
                     successful.push(path);
                   },
                   401: function ()
                   {
                     unsuccessful[path] = lg.ERROR_WRITING_PERM;
                   },
                   403: function ()
                   {
                     unsuccessful[path] = lg.ERROR_WRITING_PERM;
                   },
                   404: function ()
                   {
                     unsuccessful[path] = lg.ERROR_NO_SUCH_ITEM;
                   },
                   500: function ()
                   {
                     unsuccessful[path] = lg.ERROR_SERVER;
                   }
                 }
               })
          .always(function ()
                  {
                    totalCompleteCount++;
                  });
      }

      while (totalCompleteCount < deleteCount)
      {
        // Wait
      }

      return ($.isEmptyObject(unsuccessful));
    };

    var btns = [];
    btns.push({
                "title": lg.yes,
                "value": true,
                "classes": "btn btn-danger"
              });
    btns.push({
                "title": lg.no,
                "value": false,
                "classes": "btn btn-default"
              });

    if (deleteCount > 0)
    {
      $.prompt.disableStateButtons("deletion");

      $.prompt({
                 confirmation: {
                   html: msg,
                   buttons: btns,
                   submit: function (e, v, m, f)
                   {
                     if (v === true)
                     {
                       e.preventDefault();
                       $.prompt.nextState(function (event)
                                          {
                                            event.preventDefault();
                                            var nextState = doDelete(event)
                                              ? "successful" : "unsuccessful";
                                            $.prompt.goToState(nextState);
                                            return false;
                                          });
                       return false;
                     }
                     else
                     {
                       $.prompt.close();
                     }
                   }
                 },
                 deletion: {
                   html: lg.deleting_message.replace('%d', deleteCount)
                 },
                 successful: {
                   html: lg.successful_delete,
                   buttons: [{
                     "title": lg.close,
                     "value": false,
                     "classes": "btn btn-success"
                   }],
                   submit: refreshPage
                 },
                 unsuccessful: {
                   html: function ()
                   {
                     var output = "";

                     $.each(unsuccessful, function (path, error)
                     {
                       output += path + ": " + error + "<br />";
                     });

                     return lg.unsuccessful_delete + "<br />" + output;
                   }
                 }
               });
    }

    return isDeleted;
  };

  $(document).on("click", ".download-dropdown-menu > li > a",
                 function ()
                 {
                   var $thisLink = $(this);

                   var downloadMethod =
                     config.download.methods[$thisLink.attr("class")];
                   var form = document.createElement("form");
                   form.setAttribute("method", "POST");
                   form.setAttribute("action", contextPath
                                               + config.download.connector);

                   var methodHiddenField = document.createElement("input");
                   methodHiddenField.setAttribute("type", "hidden");
                   methodHiddenField.setAttribute("name", "method");
                   methodHiddenField.setAttribute("value", downloadMethod.id);

                   form.appendChild(methodHiddenField);

                   //Move the submit function to another variable
                   //so that it doesn't get overwritten.
                   form._submit_function_ = form.submit;

                   $.each($dt.rows({selected: true}).data(),
                          function (key, itemData)
                          {
                            var hiddenField = document.createElement("input");
                            hiddenField.setAttribute("type", "hidden");
                            hiddenField.setAttribute("name", "uri");
                            hiddenField.setAttribute("value",
                                                     config.download.vos_prefix
                                                     + itemData[9]);

                            form.appendChild(hiddenField);
                          });

                   document.body.appendChild(form);

                   form._submit_function_();
                 });

// Display an 'edit' link for editable files
// Then let user change the content of the file
// Save action is handled by the method using ajax
  var editItem = function (data)
  {
    isEdited = false;

    $fileInfo.find('div#tools').append(' <a id="edit-file" href="#" title="' +
                                       lg.edit + '"><span>' + lg.edit +
                                       '</span></a>');

    $('#edit-file').click(function ()
                          {

                            $(this).hide(); // hiding Edit link

                            var d = new Date(); // to prevent IE cache issues
                            var connectString = fileConnector +
                                                '?mode=editfile&path=' +
                                                encodeURIComponent(data['Path']) +
                                                '&config=' + userconfig +
                                                '&time=' + d.getMilliseconds();

                            $.ajax({
                                     type: 'GET',
                                     url: connectString,
                                     dataType: 'json',
                                     async: false,
                                     success: function (result)
                                     {
                                       if (result['Code'] == 0)
                                       {

                                         var content = '<form id="edit-form">';
                                         content +=
                                           '<textarea id="edit-content" name="content">' +
                                           result['Content'] + '</textarea>';
                                         content +=
                                           '<input type="hidden" name="mode" value="savefile" />';
                                         content +=
                                           '<input type="hidden" name="path" value="' +
                                           data['Path'] + '" />';
                                         content +=
                                           '<button id="edit-cancel" class="edition" type="button">' +
                                           lg.quit_editor + '</button>';
                                         content +=
                                           '<button id="edit-save" class="edition" type="button">' +
                                           lg.save + '</button>';
                                         content += '</form>';

                                         $('#preview').find('img').hide();
                                         $('#preview').prepend(content).hide().fadeIn();

                                         // Cancel Button Behavior
                                         $('#edit-cancel').click(function ()
                                                                 {
                                                                   $('#preview').find('form#edit-form').hide();
                                                                   $('#preview').find('img').fadeIn();
                                                                   $('#edit-file').show();
                                                                 });

                                         // Save Button Behavior
                                         $('#edit-save').click(function ()
                                                               {

                                                                 // we get new
                                                                 // textarea
                                                                 // content
                                                                 var newcontent = codeMirrorEditor.getValue();
                                                                 $("textarea#edit-content").val(newcontent);

                                                                 var postData = $('#edit-form').serializeArray();

                                                                 $.ajax({
                                                                          type: 'POST',
                                                                          url: fileConnector +
                                                                               '?config=' +
                                                                               userconfig,
                                                                          dataType: 'json',
                                                                          data: postData,
                                                                          async: false,
                                                                          success: function (result)
                                                                          {
                                                                            if (result['Code'] ==
                                                                                0)
                                                                            {
                                                                              isEdited =
                                                                                true;
                                                                              // if
                                                                              // (config.options.showConfirmation)
                                                                              // $.prompt(lg.successful_edit);
                                                                              $.prompt(lg.successful_edit);
                                                                            }
                                                                            else
                                                                            {
                                                                              isEdited =
                                                                                false;
                                                                              $.prompt(result['Error']);
                                                                            }
                                                                          }
                                                                        });

                                                               });

                                         // we instantiate codeMirror according
                                         // to config options
                                         codeMirrorEditor =
                                           instantiateCodeMirror(getExtension(data['Path']), config);


                                       }
                                       else
                                       {
                                         isEdited = false;
                                         $.prompt(result['Error']);
                                         $(this).show(); // hiding Edit link
                                       }
                                     }
                                   });

                          });

    return isEdited;
  };


  /*---------------------------------------------------------
   Functions to Update the File Tree
   ---------------------------------------------------------*/

// Adds a new node as the first item beneath the specified
// parent node. Called after a successful file upload.
  var addNode = function (path, name)
  {
    var ext = getExtension(name);
    var thisNode = $('#filetree').find('a[data-path="' + path + '"]');
    var parentNode = thisNode.parent();
    var newNode = '<li class="file ext_' + ext + '"><a data-path="' + path +
                  name + '" href="#" class="">' + name + '</a></li>';

    // if is root folder
    // TODO optimize
    if (!parentNode.find('ul').size())
    {
      parentNode = $('#filetree').find('ul.jqueryFileTree');

      parentNode.prepend(newNode);
    }
    else
    {
      parentNode.find('ul').prepend(newNode);
      thisNode.click().click();
    }

    getFolderInfo(path); // update list in main window

    if (config.options.showConfirmation)
    {
      $.prompt(lg.successful_added_file);
    }
  };

  /*---------------------------------------------------------
   Functions to Retrieve File and Folder Details
   ---------------------------------------------------------*/

// Decides whether to retrieve file or folder info based on
// the path provided.
  var getDetailView = function (path)
  {
    if (path.lastIndexOf('/') == path.length - 1)
    {
      getFolderInfo(path);
    }
    else
    {
      getFileInfo(path);
    }
  };

// Retrieves information about the specified file as a JSON
// object and uses that data to populate a template for
// detail views. Binds the toolbar for that detail view to
// enable specific actions. Called whenever an item is
// clicked in the file tree or list views.
  var getFileInfo = function (file)
  {
    //Hide context menu
    $('.contextMenu').hide();

    // Update location for status, upload, & new folder functions.
    var currentpath = file.substr(0, file.lastIndexOf('/') + 1);
    setUploader(currentpath);

    // Include the template.
    var template = '<div id="preview"><img /><div id="main-title"><h1></h1><div id="tools"></div></div><dl></dl></div>';
    template += '<form id="toolbar">';
    template += '<button id="parentfolder">' + lg.parentfolder + '</button>';
    if ($.inArray('select', capabilities) != -1 &&
        ($.urlParam('CKEditor') || window.opener || window.tinyMCEPopup ||
         $.urlParam('field_name')))
    {
      template +=
        '<button id="select" name="select" type="button" value="Select">' +
        lg.select + '</button>';
    }
    if ($.inArray('download', capabilities) != -1)
    {
      template +=
        '<button id="download" name="download" type="button" value="Download">' +
        lg.download + '</button>';
    }
    if ($.inArray('rename', capabilities) != -1 &&
        config.options.browseOnly != true)
    {
      template +=
        '<button id="rename" name="rename" type="button" value="Rename">' +
        lg.rename + '</button>';
    }
    if ($.inArray('move', capabilities) != -1 &&
        config.options.browseOnly != true)
    {
      template +=
        '<button id="move" name="move" type="button" value="Move">' + lg.move +
        '</button>';
    }
    if ($.inArray('delete', capabilities) != -1 &&
        config.options.browseOnly != true)
    {
      template +=
        '<button id="delete" name="delete" type="button" value="Delete">' +
        lg.del + '</button>';
    }
    if ($.inArray('replace', capabilities) != -1 &&
        config.options.browseOnly != true)
    {
      template +=
        '<button id="replace" name="replace" type="button" value="Replace">' +
        lg.replace + '</button>';
      template +=
        '<div class="hidden-file-input"><input id="fileR" name="fileR" type="file" /></div>';
      template +=
        '<input id="mode" name="mode" type="hidden" value="replace" /> ';
      template += '<input id="newfilepath" name="newfilepath" type="hidden" />';
    }
    template += '</form>';

    // test if scrollbar plugin is enabled
    if ($fileInfo.has('.mCSB_container'))
    {
      $fileInfo.find('.mCSB_container').html(template);
    }
    else
    {
      $fileInfo.html(template);
    }

    $('#parentfolder').click(function ()
                             {
                               getFolderInfo(currentpath);
                             });

    // Retrieve the data & populate the template.
    var d = new Date(); // to prevent IE cache issues
    $.getJSON(fileConnector + '?mode=getinfo&path=' + encodeURIComponent(file) +
              '&config=' + userconfig + '&time=' +
              d.getMilliseconds(), function (data)
              {
                if (data['Code'] == 0)
                {
                  $fileInfo.find('h1').text(data['Filename']).attr('title', file);

                  $fileInfo.find('img').attr('src', data['Preview']);
                  if (isVideoFile(data['Filename']) &&
                      config.videos.showVideoPlayer == true)
                  {
                    getVideoPlayer(data);
                  }
                  if (isAudioFile(data['Filename']) &&
                      config.audios.showAudioPlayer == true)
                  {
                    getAudioPlayer(data);
                  }
                  //Pdf
                  if (isPdfFile(data['Filename']) &&
                      config.pdfs.showPdfReader == true)
                  {
                    getPdfReader(data);
                  }
                  if (isEditableFile(data['Filename']) &&
                      config.edit.enabled == true && data['Protected'] == 0)
                  {
                    editItem(data);
                  }

                  // copy URL instructions - zeroclipboard
                  var d = new Date(); // to prevent IE cache issues
                  var url;

                  if (config.options.baseUrl !== false)
                  {
                    url =
                      smartPath(baseUrl, data['Path'].replace(fileRoot, ""));
                  }
                  else
                  {
                    url = data['Path'];
                  }

                  if (data['Protected'] == 0)
                  {
                    $fileInfo.find('div#tools').append(' <a id="copy-button" data-clipboard-text="' +
                                                       url + '" title="' +
                                                       lg.copy_to_clipboard +
                                                       '" href="#"><span>' +
                                                       lg.copy_to_clipboard +
                                                       '</span></a>');
                    // loading zeroClipboard code

                    loadJS(contextPath + 'scripts/zeroclipboard/copy.js?d' +
                           d.getMilliseconds());
                    $('#copy-button').click(function ()
                                            {
                                              $fileInfo.find('div#tools').append('<span id="copied">' +
                                                                                 lg.copied +
                                                                                 '</span>');
                                              $('#copied').delay(500).fadeOut(1000, function ()
                                              {
                                                $(this).remove();
                                              });
                                            });
                  }

                  var properties = '';

                  if (data['Properties']['Width'] &&
                      data['Properties']['Width'] != '')
                  {
                    properties +=
                      '<dt>' + lg.dimensions + '</dt><dd>' +
                      data['Properties']['Width'] + 'x' +
                      data['Properties']['Height'] + '</dd>';
                  }
                  if (data['Properties']['Date Created'] &&
                      data['Properties']['Date Created'] != '')
                  {
                    properties +=
                      '<dt>' + lg.created + '</dt><dd>' +
                      data['Properties']['Date Created'] + '</dd>';
                  }
                  if (data['Properties']['Date Modified'] &&
                      data['Properties']['Date Modified'] != '')
                  {
                    properties +=
                      '<dt>' + lg.modified + '</dt><dd>' +
                      data['Properties']['Date Modified'] + '</dd>';
                  }
                  if (data['Properties']['Size'] ||
                      parseInt(data['Properties']['Size']) == 0)
                  {
                    properties +=
                      '<dt>' + lg.size + '</dt><dd>' +
                      formatBytes(data['Properties']['Size']) + '</dd>';
                  }
                  $fileInfo.find('dl').html(properties);

                  // Bind toolbar functions.
                  bindToolbar(data);

                }
                else
                {
                  $.prompt(data['Error']);
                }
              });
  };

// Retrieves data for all items within the given folder and
// creates a list view. Binds contextual menu options.
// TODO: consider stylesheet switching to switch between grid
// and list views with sorting options.
  var getFolderInfo = function (path)
  {
    // Update location for status, upload, & new folder functions.
    setUploader(path);
  };

  /*---------------------------------------------------------
   Initialization
   ---------------------------------------------------------*/

  $(function ()
    {
      if (config.extras.extra_js)
      {
        for (var i = 0; i < config.extras.extra_js.length; i++)
        {
          $.ajax({
                   url: config.extras.extra_js[i],
                   dataType: "script",
                   async: config.extras.extra_js_async
                 });
        }
      }

      $('#link-to-project').attr('href', config.url).attr('target', '_blank').attr('title', lg.support_fm +
                                                                                            ' [' +
                                                                                            lg.version +
                                                                                            ' : ' +
                                                                                            config.version +
                                                                                            ']');
      $('div.version').html(config.version);

      // Loading theme
      loadCSS(contextPath + 'themes/' + config.options.theme +
              '/styles/filemanager.css');
      $.ajax({
               url: contextPath + 'themes/' + config.options.theme +
                    '/styles/ie.css',
               async: false,
               success: function (data)
               {
                 $('head').append(data);
               }
             });

      // Loading quota and folder size for root folder, e.g. /CADCTest
      var currPath = getCurrentPath();
      if (stringUtil.hasText(currPath))
      {
        $.ajax({
                 method: 'GET',
                 url: contextPath + config.options.folderConnector + '/' +
                      currPath.split('/')[1],
                 dataType: 'json',
                 async: false,
                 success: function (data)
                 {
                   var htmlString = stringUtil.format(
                     '<strong >{1}</strong> remaining of <strong>{2}</strong> <span class="request-more-link">(<a href="mailto:support@canfar.net">Request more</a>)</span>',
                     [data.size, data.quota]);
                   $('div.quota').html(htmlString);
                 }
               });
      }

      // loading zeroClipboard
      loadJS(contextPath + 'scripts/zeroclipboard/dist/ZeroClipboard.js');

      // Loading CodeMirror if enabled for online edition
      if (config.edit.enabled)
      {
        loadCSS(contextPath + 'scripts/CodeMirror/lib/codemirror.css');
        loadCSS(contextPath + 'scripts/CodeMirror/theme/' + config.edit.theme +
                '.css');
        loadJS(contextPath + 'scripts/CodeMirror/lib/codemirror.js');
        loadJS(contextPath +
               'scripts/CodeMirror/addon/selection/active-line.js');
        loadCSS(contextPath +
                'scripts/CodeMirror/addon/display/fullscreen.css');
        loadJS(contextPath + 'scripts/CodeMirror/addon/display/fullscreen.js');
        loadJS(contextPath + 'scripts/CodeMirror/dynamic-mode.js');
      }

      if (!config.options.fileRoot)
      {
        fileRoot = '/' +
                   document.location.pathname.substring(1, document.location.pathname.lastIndexOf('/') +
                   1) + 'userfiles/';
      }
      else
      {
        if (!config.options.serverRoot)
        {
          fileRoot = config.options.fileRoot;
        }
        else
        {
          fileRoot = '/' + config.options.fileRoot;
        }
        // we remove double slashes - can happen when using PHP SetFileRoot()
        // function with fileRoot = '/' value
        fileRoot = fileRoot.replace(/\/\//g, '\/');
      }

      if (config.options.baseUrl === false)
      {
        baseURL = window.location.protocol + "//" + window.location.host;
      }
      else
      {
        baseURL = config.options.baseUrl;
      }

      if ($.urlParam('exclusiveFolder') != 0)
      {
        fileRoot += $.urlParam('exclusiveFolder');
        if (fileRoot.charAt(fileRoot.length - 1) != '/')
        {
          fileRoot += '/';
        } // add last '/' if needed
        fileRoot = fileRoot.replace(/\/\//g, '\/');
      }

      if ($.urlParam('expandedFolder') != 0)
      {
        expandedFolder = $.urlParam('expandedFolder');
        fullexpandedFolder = fileRoot + expandedFolder;
      }
      else
      {
        expandedFolder = '';
        fullexpandedFolder = null;
      }

      var $itemOptions = $("#itemOptions");

      $('#folder-info').html('<span id="items-counter"></span> ' + lg.items +
                             ' - ' + lg.size +
                             ' : <span id="items-size"></span> ' + lg.mb);

      // we finalize the FileManager UI initialization
      // with localized text if necessary
      if (config.options.autoload == true)
      {
        $fileInfo.find('h1').append(lg.select_from_left);
        $itemOptions.find('a[href$="#select"]').append(lg.select);
        $itemOptions.find('a[href$="#download"]').append(lg.download);
        $itemOptions.find('a[href$="#rename"]').append(lg.rename);
        $itemOptions.find('a[href$="#move"]').append(lg.move);
        $itemOptions.find('a[href$="#replace"]').append(lg.replace);
        $itemOptions.find('a[href$="#delete"]').append(lg.del);
      }

      /** Adding a close button triggering callback function if CKEditorCleanUpFuncNum passed */
      if ($.urlParam('CKEditorCleanUpFuncNum'))
      {
        $("body").append('<button id="close-btn" type="button">' + lg.close +
                         '</button>');

        $('#close-btn').click(function ()
                              {
                                parent.CKEDITOR.tools.callFunction($.urlParam('CKEditorCleanUpFuncNum'));
                              });
      }

      /** Input file Replacement */
      $("#newfile").change(function ()
                           {
                             $("#filepath").val($(this).val().replace(/.+[\\\/]/, ""));
                           });

      /** load searchbox */
      if (config.options.searchBox === true)
      {
        loadJS(contextPath + 'scripts/filemanager.liveSearch.js');
      }
      else
      {
        $('#search').remove();
      }

      // cosmetic tweak for buttons
      $('button').wrapInner('<span></span>');

      // Provide initial values for upload form, status, etc.
      setUploader(fileRoot);

      // Handling File upload

      // Multiple Uploads
      if (config.upload.multiple)
      {

        // we load dropzone library
        loadCSS(contextPath + 'scripts/dropzone/downloads/css/dropzone.css');
        loadJS(contextPath + 'scripts/dropzone/downloads/dropzone.js');
        Dropzone.autoDiscover = false;

        // we remove simple file upload element
        $('#file-input-container').remove();

        // we add multiple-files upload button using upload button
        // $('#upload').prop('type', 'button');
        // replaced by code below because og Chrome 18 bug
        // https://github.com/simogeo/Filemanager/issues/304 and it may also be
        // safer for IE (see
        // http://stackoverflow.com/questions/1544317/change-type-of-input-field-with-jquery

        $('#upload').off().click(function ()
                                 {
                                   // Create prompt window: path is a hidden
                                   // element in a form embedded in the New
                                   // menu dropdown.
                                   var msg = '<div id="dropzone-container"><h2>' +
                                             lg.current_folder +
                                             $('#currentFolderName').val() +
                                             '</h2><div id="multiple-uploads" class="dropzone"></div>';
                                   msg +=
                                     '<div id="total-progress"><div data-dz-uploadprogress="" style="width:0;" class="progress-bar"></div></div>';
                                   msg += '<div class="prompt-info">' +
                                          lg.dz_dictMaxFilesExceeded.replace('%s', config.upload.number) +
                                          lg.file_size_limit +
                                          config.upload.fileSizeLimit + ' ' +
                                          lg.mb + '.</div>';
                                   msg += '<button id="process-upload">' +
                                          lg.upload + '</button></div>';

                                   var error_flag = false;
                                   var path = getCurrentPath();

                                   var btns = {};
                                   btns[lg.close] = false;
                                   $.prompt(msg, {
                                     buttons: btns
                                   });

                                   var $progressBar =
                                     $("#total-progress").find(".progress-bar");
                                   var $uploadResponse = $("#uploadresponse");

                                   $("div#multiple-uploads").dropzone({
                                                                        paramName: "upload",
                                                                        url: contextPath +
                                                                             config.options.fileConnector +
                                                                             path,
                                                                        method: 'put',
                                                                        maxFilesize: config.upload.fileSizeLimit,  // 10GB max.
                                                                        maxFiles: config.upload.number,
                                                                        addRemoveLinks: true,
                                                                        parallelUploads: config.upload.number,
                                                                        dictCancelUpload: lg.cancel,
                                                                        dictRemoveFile: lg.del,
                                                                        dictMaxFilesExceeded: lg.dz_dictMaxFilesExceeded.replace("%s", config.upload.number),
                                                                        dictDefaultMessage: lg.dz_dictDefaultMessage,
                                                                        acceptedFiles: null,
                                                                        autoProcessQueue: false,
                                                                        init: function ()
                                                                        {
                                                                          // for
                                                                          // accessing
                                                                          // dropzone
                                                                          // :
                                                                          // https://github.com/enyo/dropzone/issues/180
                                                                          var dropzone = this;
                                                                          $("#process-upload").click(function ()
                                                                                                     {
                                                                                                       // To proceed full queue parallelUploads must be equal or > to maxFileSize. https://github.com/enyo/dropzone/issues/462
                                                                                                       dropzone.processQueue();
                                                                                                     });
                                                                        },
                                                                        totaluploadprogress: function (progress)
                                                                        {
                                                                          $progressBar.css('width', progress +
                                                                                                    "%");
                                                                        },
                                                                        sending: function (file, xhr, formData)
                                                                        {
                                                                          formData.append("mode", "add");
                                                                          formData.append("currentpath", path);
                                                                        },
                                                                        error: function ()
                                                                        {
                                                                          error_flag =
                                                                            true;
                                                                        },
                                                                        success: function (file, jsonResponse)
                                                                        {
                                                                          $uploadResponse.empty().text(jsonResponse);

                                                                          if (jsonResponse.code ==
                                                                              0)
                                                                          {
                                                                            this.removeFile(file);
                                                                          }
                                                                          else
                                                                          {
                                                                            getFolderInfo(path);
                                                                            $.prompt(jsonResponse.error);
                                                                            error_flag =
                                                                              true;
                                                                          }
                                                                        },
                                                                        complete: function ()
                                                                        {
                                                                          if ((this.getUploadingFiles().length ===
                                                                               0)
                                                                              &&
                                                                              (this.getQueuedFiles().length ===
                                                                               0))
                                                                          {
                                                                            $progressBar.css('width', '0%');

                                                                            if (error_flag ===
                                                                                true)
                                                                            {
                                                                              var rejects = this.getRejectedFiles();

                                                                              for (var rfi = 0, rfl = rejects.length;
                                                                                   rfi <
                                                                                   rfl;
                                                                                   rfi++)
                                                                              {

                                                                              }

                                                                              $.prompt(lg.unsuccessful_added_file);
                                                                            }
                                                                            else if (config.options.showConfirmation)
                                                                            {
                                                                              $.prompt(lg.successful_added_file, {
                                                                                submit: function ()
                                                                                {
                                                                                  refreshPage();
                                                                                }
                                                                              });
                                                                            }
                                                                            else
                                                                            {
                                                                              refreshPage();
                                                                            }
                                                                          }

                                                                          // Reset.
                                                                          error_flag =
                                                                            false;
                                                                        }
                                                                      });

                                 });

        // Simple Upload
      }
      else
      {

        $('#uploader').attr('action', fileConnector + '?config=' + userconfig);

        $('#uploader').ajaxForm({
                                  target: '#uploadresponse',
                                  beforeSubmit: function (arr, form, options)
                                  {
                                    // Test if a value is given
                                    if ($('#newfile', form).val() == '')
                                    {
                                      return false;
                                    }
                                    // Check if file extension is allowed
                                    if (!isAuthorizedFile($('#newfile', form).val()))
                                    {
                                      var str = '<p>' + lg.INVALID_FILE_TYPE +
                                                '</p>';
                                      if (config.security.uploadPolicy ==
                                          'DISALLOW_ALL')
                                      {
                                        str += '<p>' + lg.ALLOWED_FILE_TYPE +
                                               config.security.uploadRestrictions.join(', ') +
                                               '.</p>';
                                      }
                                      if (config.security.uploadPolicy ==
                                          'ALLOW_ALL')
                                      {
                                        str += '<p>' + lg.DISALLOWED_FILE_TYPE +
                                               config.security.uploadRestrictions.join(', ') +
                                               '.</p>';
                                      }
                                      $("#filepath").val('');
                                      $.prompt(str);
                                      return false;
                                    }
                                    $('#upload').attr('disabled', true);
                                    $('#upload span').addClass('loading').text(lg.loading_data);
                                    if ($.urlParam('type').toString().toLowerCase() ==
                                        'images')
                                    {
                                      // Test if uploaded file extension is in
                                      // valid image extensions
                                      var newfileSplitted = $('#newfile', form).val().toLowerCase().split('.');
                                      var found = false;
                                      for (key in config.images.imagesExt)
                                      {
                                        if (config.images.imagesExt[key] ==
                                            newfileSplitted[newfileSplitted.length -
                                                            1])
                                        {
                                          found = true;
                                        }
                                      }
                                      if (found === false)
                                      {
                                        $.prompt(lg.UPLOAD_IMAGES_ONLY);
                                        $('#upload').removeAttr('disabled').find("span").removeClass('loading').text(lg.upload);
                                        return false;
                                      }
                                    }
                                    // if config.upload.fileSizeLimit == auto
                                    // we delegate size test to connector
                                    if (typeof FileReader !== "undefined" &&
                                        typeof config.upload.fileSizeLimit !=
                                        "auto")
                                    {
                                      // Check file size using html5 FileReader
                                      // API
                                      var size = $('#newfile', form).get(0).files[0].size;
                                      if (size >
                                          config.upload.fileSizeLimit * 1024 *
                                          1024)
                                      {
                                        $.prompt("<p>" + lg.file_too_big +
                                                 "</p><p>" +
                                                 lg.file_size_limit +
                                                 config.upload.fileSizeLimit +
                                                 " " + lg.mb + ".</p>");
                                        $('#upload').removeAttr('disabled').find("span").removeClass('loading').text(lg.upload);
                                        return false;
                                      }
                                    }


                                  },
                                  error: function (jqXHR, textStatus, errorThrown)
                                  {
                                    $('#upload').removeAttr('disabled').find("span").removeClass('loading').text(lg.upload);
                                    $.prompt(lg.ERROR_UPLOADING_FILE);
                                  },
                                  success: function (result)
                                  {
                                    var data = jQuery.parseJSON($('#uploadresponse').find('textarea').text());
                                    if (data['Code'] == 0)
                                    {
                                      // addNode(data['Path'], data['Name']);
                                      $("#filepath, #newfile").val('');
                                      // IE can not empty input='file'. A fix
                                      // consist to replace the element (see
                                      // github issue #215)
                                      if ($.browser.msie)
                                      {
                                        $("#newfile").replaceWith($("#newfile").clone(true));
                                      }
                                    }
                                    else
                                    {
                                      $.prompt(data['Error']);
                                    }
                                    $('#upload').removeAttr('disabled');
                                    $('#upload span').removeClass('loading').text(lg.upload);
                                    $("#filepath").val('');
                                  }
                                });
      }

      // Loading CustomScrollbar if enabled
      // Important, the script should be called after calling createFileTree()
      // to prevent bug
      if (config.customScrollbar.enabled)
      {
        loadCSS(contextPath +
                'scripts/custom-scrollbar-plugin/jquery.mCustomScrollbar.min.css');
        loadJS(contextPath +
               'scripts/custom-scrollbar-plugin/jquery.mCustomScrollbar.concat.min.js');

        var csTheme = config.customScrollbar.theme != undefined ?
                      config.customScrollbar.theme : 'inset-2-dark';
        var csButton = config.customScrollbar.button != undefined ?
                       config.customScrollbar.button : true;

        $(window).load(function ()
                       {
                         $("#fileinfo").mCustomScrollbar({
                                                           theme: csTheme,
                                                           scrollButtons: {enable: csButton},
                                                           advanced: {
                                                             autoExpandHorizontalScroll: true,
                                                             updateOnContentResize: true
                                                           },
                                                           axis: "y",
                                                           alwaysShowScrollbar: 1
                                                         });

                       });
      }

      // Disable select function if no window.opener
      if (!(window.opener || window.tinyMCEPopup ||
            $.urlParam('field_name')))
      {
        $('#itemOptions a[href$="#select"]').remove();
      }
      // Keep only browseOnly features if needed
      if (config.options.browseOnly == true)
      {
        $('#file-input-container').remove();
        $('#upload').remove();
        $('#newfolder').remove();
        $('#toolbar').remove('#rename');
        $('.contextMenu .rename').remove();
        $('.contextMenu .move').remove();
        $('.contextMenu .replace').remove();
        $('.contextMenu .delete').remove();
      }

      // Adjust layout.
      setDimensions();
      $(window).resize(setDimensions);

      setMover();
      setLinker();
      getDetailView(fileRoot + expandedFolder);
    });

// add useragent string to html element for IE 10/11 detection
  var doc = document.documentElement;
  doc.setAttribute('data-useragent', navigator.userAgent);

  if (config.options.logger)
  {
    var end = new Date().getTime();
    var time = end - start;
    console.log('Total execution time : ' + time + ' ms');
  }

  $(window).load(function ()
                 {
                   setDimensions();
                 });

}
