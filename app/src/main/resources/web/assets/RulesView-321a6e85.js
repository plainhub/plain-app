import{d as le,B as ce,r as T,u as se,a2 as J,al as R,c2 as V,c3 as re,ah as ue,I as pe,ai as F,o as r,e as u,f as e,j as o,k as c,M as _,c4 as N,c5 as L,F as w,A as C,N as X,aj as Z,x as A,y as x,m as O,ao as me,i as _e,t as ve,a6 as j,c6 as fe,c7 as he,L as ge,P as ee,g as te,Q as oe,w as ne,V as B,a7 as $e,Y as be}from"./index-2c8e7849.js";import{_ as ke}from"./Breadcrumb-01ba2071.js";import{T as $,a as b,_ as ye,A as we}from"./question-mark-rounded-bae86836.js";import{u as Ce,a as Ve}from"./vee-validate.esm-83a41e33.js";import"./stringToArray-36968540.js";const Fe={slot:"headline"},Ne={slot:"content"},Te={class:"row"},Ae={class:"col-md-3 col-form-label"},De={class:"col-md-9 form-checks"},Ie={class:"form-check form-check-inline"},Me={class:"form-check-label",for:"action-allow"},Ee={class:"form-check form-check-inline"},Se={class:"form-check-label",for:"action-block"},Ue={class:"row mb-2"},qe={for:"action",class:"col-md-3 col-form-label"},Re={class:"col-md-9 form-checks"},Le={class:"form-check form-check-inline"},Oe={class:"form-check-label",for:"direction-inbound"},je={class:"form-check form-check-inline"},Be={class:"form-check-label",for:"direction-outbound"},Je={class:"row mb-3"},Qe={class:"col-md-3 col-form-label"},Ge={class:"col-md-9"},Pe=["value"],Ye={key:0,class:"input-group mt-2"},ze=["placeholder"],He={class:"inner"},Ke={class:"help-block"},We={value:""},Xe=["value"],Ze={key:2,class:"invalid-feedback"},xe={class:"row mb-3"},et={class:"col-md-3 col-form-label"},tt={class:"col-md-9"},ot={value:"all"},nt=["value"],at=["value"],lt={class:"row mb-3"},st={class:"col-md-3 col-form-label"},it={class:"col-md-9"},dt={slot:"actions"},ct=["disabled"],ae=le({__name:"EditRuleModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(k){var y,Q,G,P,Y,z,H;const f=k,{handleSubmit:g}=Ce(),l=ce({action:"block",direction:"inbound",protocol:"all",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=T($.DNS),D=Object.values($),{t:I}=se(),{mutate:M,loading:E,onDone:S}=J({document:R`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${V}
  `,options:{update:(n,s)=>{re(n,s.data.createConfig,R`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${V}
        `)}}}),{mutate:U,loading:q,onDone:a}=J({document:R`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${V}
  `}),{value:m,resetField:v,errorMessage:h}=Ve("inputValue",ue().test("required",n=>"valid.required",n=>!b.hasInput(p.value)||!!n).test("target-value",n=>"invalid_value",n=>b.isValid(p.value,n??""))),i=(y=f.data)==null?void 0:y.data;l.action=(i==null?void 0:i.action)??"block",l.direction=(i==null?void 0:i.direction)??"inbound",l.protocol=(i==null?void 0:i.protocol)??"all",p.value=((G=(Q=f.data)==null?void 0:Q.target)==null?void 0:G.type)??$.DNS,m.value=((Y=(P=f.data)==null?void 0:P.target)==null?void 0:Y.value)??"",l.apply_to=((H=(z=f.data)==null?void 0:z.applyTo)==null?void 0:H.toValue())??"all",l.notes=(i==null?void 0:i.notes)??"",l.is_enabled=(i==null?void 0:i.is_enabled)??!0,i||v(),pe(p,(n,s)=>{(n===$.INTERFACE||s===$.INTERFACE)&&(m.value="")});const d=g(()=>{const n=new b;n.type=p.value,n.value=m.value??"",l.target=n.toValue(),f.data?U({id:f.data.id,input:{group:"rule",value:JSON.stringify(l)}}):M({input:{group:"rule",value:JSON.stringify(l)}})});return S(()=>{F()}),a(()=>{F()}),(n,s)=>{var K,W;const ie=ye,de=me;return r(),u("md-dialog",null,[e("div",Fe,o(c(i)?n.$t("edit"):n.$t("create")),1),e("div",Ne,[e("div",Te,[e("label",Ae,o(n.$t("actions")),1),e("div",De,[e("div",Ie,[_(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-allow",value:"allow","onUpdate:modelValue":s[0]||(s[0]=t=>l.action=t)},null,512),[[N,l.action]]),e("label",Me,o(n.$t("allow")),1)]),e("div",Ee,[_(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-block",value:"block","onUpdate:modelValue":s[1]||(s[1]=t=>l.action=t)},null,512),[[N,l.action]]),e("label",Se,o(n.$t("block")),1)])])]),e("div",Ue,[e("label",qe,o(n.$t("direction")),1),e("div",Re,[e("div",Le,[_(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-inbound",value:"inbound","onUpdate:modelValue":s[2]||(s[2]=t=>l.direction=t)},null,512),[[N,l.direction]]),e("label",Oe,o(n.$t("inbound")),1)]),e("div",je,[_(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-outbound",value:"outbound","onUpdate:modelValue":s[3]||(s[3]=t=>l.direction=t)},null,512),[[N,l.direction]]),e("label",Be,o(n.$t("outbound")),1)])])]),e("div",Je,[e("label",Qe,o(n.$t("match")),1),e("div",Ge,[_(e("select",{class:"form-select","onUpdate:modelValue":s[4]||(s[4]=t=>p.value=t)},[(r(!0),u(w,null,C(c(D),t=>(r(),u("option",{value:t},o(n.$t(`target_type.${t}`)),9,Pe))),256))],512),[[L,p.value]]),c(b).hasInput(p.value)?(r(),u("div",Ye,[_(e("input",{type:"text",class:"form-control","onUpdate:modelValue":s[5]||(s[5]=t=>Z(m)?m.value=t:null),placeholder:n.$t("for_example")+" "+c(b).hint(p.value)},null,8,ze),[[X,c(m)]]),A(de,{class:"input-group-text"},{content:x(()=>[e("pre",Ke,o(n.$t(`examples_${p.value}`)),1)]),default:x(()=>[e("span",He,[A(ie)])]),_:1})])):O("",!0),p.value===c($).INTERFACE?_((r(),u("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":s[6]||(s[6]=t=>Z(m)?m.value=t:null)},[e("option",We,o(n.$t("all_local_networks")),1),(r(!0),u(w,null,C((K=k.networks)==null?void 0:K.filter(t=>t.type!=="wan"),t=>(r(),u("option",{value:t.ifName},o(t.name),9,Xe))),256))],512)),[[L,c(m)]]):O("",!0),c(h)?(r(),u("div",Ze,o(c(h)?n.$t(c(h)):""),1)):O("",!0)])]),e("div",xe,[e("label",et,o(c(I)("apply_to")),1),e("div",tt,[_(e("select",{class:"form-select","onUpdate:modelValue":s[7]||(s[7]=t=>l.apply_to=t)},[e("option",ot,o(n.$t("all_devices")),1),(r(!0),u(w,null,C((W=k.networks)==null?void 0:W.filter(t=>t.type!=="wan"),t=>(r(),u("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,nt))),128)),(r(!0),u(w,null,C(k.devices,t=>(r(),u("option",{value:"mac:"+t.mac},o(t.name),9,at))),256))],512),[[L,l.apply_to]])])]),e("div",lt,[e("label",st,o(n.$t("notes")),1),e("div",it,[_(e("textarea",{class:"form-control","onUpdate:modelValue":s[8]||(s[8]=t=>l.notes=t),rows:"3"},null,512),[[X,l.notes]])])])]),e("div",dt,[e("md-outlined-button",{value:"cancel",onClick:s[9]||(s[9]=(...t)=>c(F)&&c(F)(...t))},o(n.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:c(E)||c(q),onClick:s[10]||(s[10]=(...t)=>c(d)&&c(d)(...t)),autofocus:""},o(n.$t("save")),9,ct)])])}}}),rt={class:"page-container"},ut={class:"main"},pt={class:"v-toolbar"},mt={class:"table-responsive"},_t={class:"table"},vt=e("th",null,"ID",-1),ft={class:"actions two"},ht={class:"form-check"},gt=["disabled","onChange","checked"],$t={class:"nowrap"},bt={class:"nowrap"},kt={class:"actions two"},yt=["onClick"],wt=["onClick"],At=le({__name:"RulesView",setup(k){const f=T([]),g=T([]),l=T([]),{t:p}=se();_e({handle:(a,m)=>{m?ve(p(m),"error"):(f.value=a.configs.filter(v=>v.group==="rule").map(v=>{const h=JSON.parse(v.value),i=new we;i.parse(h.apply_to);const d=new b;return d.parse(h.target),{id:v.id,createdAt:v.createdAt,updatedAt:v.updatedAt,data:h,applyTo:i,target:d}}),g.value=[...a.devices],l.value=[...a.networks])},document:j`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${fe}
    ${V}
    ${he}
  `});const D=j`
  mutation DeleteConfig($id: ID!) {
    deleteConfig(id: $id)
  }
`;function I(a){B($e,{id:a.id,name:a.id,gql:D,appApi:!1,typeName:"Config"})}function M(a){B(ae,{data:a,devices:g,networks:l})}function E(){B(ae,{data:null,devices:g,networks:l})}const{mutate:S,loading:U}=J({document:j`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${V}
  `});function q(a){S({id:a.id,input:{group:"rule",value:JSON.stringify(a.data)}})}return(a,m)=>{const v=ke,h=be,i=ge("tooltip");return r(),u("div",rt,[e("div",ut,[e("div",pt,[A(v,{current:()=>a.$t("page_title.rules")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:E},o(a.$t("create")),1)]),e("div",mt,[e("table",_t,[e("thead",null,[e("tr",null,[vt,e("th",null,o(a.$t("apply_to")),1),e("th",null,o(a.$t("description")),1),e("th",null,o(a.$t("notes")),1),e("th",null,o(a.$t("enabled")),1),e("th",null,o(a.$t("created_at")),1),e("th",null,o(a.$t("updated_at")),1),e("th",ft,o(a.$t("actions")),1)])]),e("tbody",null,[(r(!0),u(w,null,C(f.value,d=>(r(),u("tr",{key:d.id},[e("td",null,[A(h,{id:d.id,raw:d.data},null,8,["id","raw"])]),e("td",null,o(d.applyTo.getText(a.$t,g.value,l.value)),1),e("td",null,o(a.$t(`rule_${d.data.direction}`,{action:a.$t(d.data.action),target:d.target.getText(a.$t,l.value)})),1),e("td",null,o(d.data.notes),1),e("td",null,[e("div",ht,[e("md-checkbox",{"touch-target":"wrapper",disabled:c(U),onChange:y=>q(d),checked:d.data.is_enabled},null,40,gt)])]),e("td",$t,[_((r(),u("span",null,[te(o(c(oe)(d.createdAt)),1)])),[[i,c(ee)(d.createdAt)]])]),e("td",bt,[_((r(),u("span",null,[te(o(c(oe)(d.updatedAt)),1)])),[[i,c(ee)(d.updatedAt)]])]),e("td",kt,[e("a",{href:"#",class:"v-link",onClick:ne(y=>M(d),["prevent"])},o(a.$t("edit")),9,yt),e("a",{href:"#",class:"v-link",onClick:ne(y=>I(d),["prevent"])},o(a.$t("delete")),9,wt)])]))),128))])])])])])}}});export{At as default};
