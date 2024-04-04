<#if !(request.getAttribute("org.joget.marketplace.likeDislikeElement")??) >
    <script src="${request.contextPath}/plugin/org.joget.marketplace.LikeDislike/js/like-dislike.js"></script>

<#if !includeMetaData>
    <#if !id?has_content>
        <#if element.properties.hideElement != 'true'>
        <#--  When form is not submitted, hide element is not true  -->
         <div class="form-cell" ${elementMetaData!}>
            <label field-tooltip="${elementParamName!}" style="margin-top:10px !important; margin-bottom:10px !important;" class="label" for="${elementParamName!}_${element.properties.elementUniqueKey!}">${element.properties.label}</label>
            <div id="rating-wrapper" style="margin-top:10px !important; margin-bottom:10px !important;">
                <div class="content-ld" id="${elementParamName!}">
                    <button class="btn btn-default like" id="like-btn"><span class="far fa-thumbs-up" aria-hidden="true"></span> Like</button>
                    <span class="likes">${likes!}</span>
                    <#if anonymous == "false">
                        <button class="btn btn-default dislike" id="dislike-btn"><span class="far fa-thumbs-down" aria-hidden="true"></span> Dislike</button>
                        <span class="dislikes">${dislikes!}</span>
                    </#if>
                </div>
            </div>
        </div>
        </#if>
    <#else>
    <#--  When form is submitted  -->
     <div class="form-cell" ${elementMetaData!}>
        <label field-tooltip="${elementParamName!}" style="margin-top:10px !important; margin-bottom:10px !important;" class="label" for="${elementParamName!}_${element.properties.elementUniqueKey!}">${element.properties.label}</label>
        <div id="rating-wrapper" style="margin-top:10px !important; margin-bottom:10px !important;">
            <div class="content-ld" id="${elementParamName!}">
                <button class="btn btn-default like" id="like-btn"><span class="far fa-thumbs-up" aria-hidden="true"></span> Like</button>
                <span class="likes">${likes!}</span>
                <#if anonymous == "false">
                    <button class="btn btn-default dislike" id="dislike-btn"><span class="far fa-thumbs-down" aria-hidden="true"></span> Dislike</button>
                    <span class="dislikes">${dislikes!}</span>
                </#if>
            </div>
        </div>
    </div>
    </#if>
<#else>
  <#--  Form Builder  -->
    <div class="form-cell" ${elementMetaData!}>
        <label field-tooltip="${elementParamName!}" style="margin-top:10px !important; margin-bottom:10px !important;" class="label" for="${elementParamName!}_${element.properties.elementUniqueKey!}">${element.properties.label}</label>
        <div id="rating-wrapper" style="margin-top:10px !important; margin-bottom:10px !important;">
            <div class="content-ld" id="${elementParamName!}">
                <button class="btn btn-default like" id="like-btn"><span class="far fa-thumbs-up" aria-hidden="true"></span> Like</button>
                <span class="likes">${likes!}</span>
                <#if anonymous == "false">
                    <button class="btn btn-default dislike" id="dislike-btn"><span class="far fa-thumbs-down" aria-hidden="true"></span> Dislike</button>
                    <span class="dislikes">${dislikes!}</span>
                </#if>
            </div>
        </div>
    </div>
</#if>

    <#if id?has_content>
    <script>
        $("#${elementParamName!}").likeDislike({
            reverseMode: false,
            <#if anonymous == "true">
                readOnly:true,
            </#if>
            activeBtn: localStorage['key']? localStorage['key'] : '',
            click:function(btnType, likes, dislikes, event) {
                event.preventDefault();
                var clickedButtonId = event.target.id;
                var likes = $(this.element).find('.likes');
                var dislikes = $(this.element).find('.dislikes');
                $.ajax({
                    url: '${element.serviceUrl!}',
                    type: 'POST',
                    data: {
                        id: '${id}',
                        clickedValue: btnType,
                        buttonId: clickedButtonId,
                        config : '${element.configString!}'
                    },
                    success: function (data) {
                      likes.text(data.likes);
                      dislikes.text(data.dislikes);
                      localStorage['key'] = btnType;
                    }
                  });
            }
        });
    </script>
    </#if>
</#if>